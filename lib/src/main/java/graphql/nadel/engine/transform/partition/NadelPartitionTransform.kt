package graphql.nadel.engine.transform.partition

import graphql.language.OperationDefinition.Operation
import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.query.NFUtil
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.queryPath
import graphql.nadel.engine.util.toGraphQLError
import graphql.nadel.engine.util.unwrapNonNull
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLList
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLScalarType
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal class NadelPartitionTransform(
    private val engine: NextgenEngine,
    private val instructions: NadelPartitionTransformHook,
) : NadelTransform<NadelPartitionTransform.State> {
    data class State(
        val executionContext: NadelExecutionContext,
        val pathToPartitionPoint: List<String>,
        var partitionState: PartitionState = PartitionStateEmpty,
    )

    sealed interface PartitionState

    data object PartitionStateEmpty : PartitionState

    data class PartitionStateSuccess(
        val partitionCalls: List<Deferred<ServiceExecutionResult>>,
    ) : PartitionState

    data class PartitionStateError(
        val errors: MutableList<Throwable>,
    ) : PartitionState

    private val fieldPartition = NadelFieldPartition(instructions.getPartitionKeyExtractor())

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): State? {

        return if (executionContext.isPartitionedCall) {
            null
        } else {
            val pathToPartitionPoint = instructions.getPathToPartitionPoint(
                executionContext,
                serviceExecutionContext,
                executionBlueprint,
                services,
                service,
                overallField,
                hydrationDetails,
            )

            if (pathToPartitionPoint == null) {
                null
            } else {
                State(executionContext = executionContext, pathToPartitionPoint = pathToPartitionPoint)
            }
        }
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        transformer: NadelQueryTransformer,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: State,
    ): NadelTransformFieldResult {

        val fieldPartitions = try {
            fieldPartition.createFieldPartitions(
                field = field,
                pathToPartitionPoint = state.pathToPartitionPoint,
                graphQLSchema = executionBlueprint.engineSchema
            )
        } catch (exception: NadelCannotPartitionFieldException) {
            state.partitionState = PartitionStateError(errors = mutableListOf(exception))
            return NadelTransformFieldResult.unmodified(field)
        }

        val firstPartition = fieldPartitions.values.first()

        val rootType = when (executionContext.query.operation!!) {
            Operation.QUERY -> executionBlueprint.engineSchema.queryType
            Operation.MUTATION -> executionBlueprint.engineSchema.mutationType
            Operation.SUBSCRIPTION -> error("Subscriptions are not supported")
        }

        val partitionCalls = coroutineScope {
            fieldPartitions.values.drop(1).map {
                async {
                    val topLevelField = NFUtil.createField(
                        executionBlueprint.engineSchema,
                        rootType,
                        field.queryPath,
                        it.normalizedArguments,
                        it.children
                    )

                    engine.executePartitionedCall(topLevelField, service, state.executionContext)
                }
            }
        }

        state.partitionState = PartitionStateSuccess(partitionCalls = partitionCalls)

        return NadelTransformFieldResult(newField = firstPartition)
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: State,
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {

        val parentNodes = nodes.getNodesAt(
            queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root,
            flatten = true,
        )

        if (parentNodes.size != 1) {
            // TODO: Log strange log - should always be 1, right?
            return emptyList()
        }

        val nullifyField = NadelResultInstruction.Set(
            subject = parentNodes.first(),
            key = NadelResultKey(overallField.resultKey),
            newValue = null
        )

        if (state.partitionState is PartitionStateError) {
            return (state.partitionState as PartitionStateError).errors.map {
                NadelResultInstruction.AddError(
                    NadelPartitionGraphQLErrorException(
                        "The call for field '${overallField.resultKey}' was not partitioned due to the following error: '${it.message}'",
                        path = overallField.queryPath.segments,
                    )
                )
            } + nullifyField
        }

        // TODO: handle HTTP errors
        val resultFromPartitionCalls = (state.partitionState as PartitionStateSuccess).partitionCalls.awaitAll()

        val thisNodesData = nodes.getNodesAt(queryPath = overallField.queryPath, flatten = false).let {
            check(it.size == 1) { "Expected exactly one node at ${overallField.queryPath}, but found ${it.size}" }
            it.first().value
        }

        val dataFromPartitionCalls = resultFromPartitionCalls.map { resultFromPartitionCall ->
            JsonNodes(resultFromPartitionCall.data).getNodesAt(overallField.queryPath, flatten = false).let {
                check(it.size == 1) { "Expected exactly one node at ${overallField.queryPath}, but found ${it.size}" }
                it.first().value
            }
        }

        val overallFieldType = overallField.getType(executionBlueprint.engineSchema)

        val mergedData = if (overallFieldType.isList) {
            mergeDataFromList(
                dataFromPartitionCalls = dataFromPartitionCalls,
                thisNodesData = thisNodesData,
                parentNodes = parentNodes,
                overallField = overallField,
            )
        } else if (overallFieldType.isMutationPayloadLike()) {
            mergeDataFromMutationPayloadLike(
                dataFromPartitionCalls = dataFromPartitionCalls,
                thisNodesData = thisNodesData,
                parentNodes = parentNodes,
                overallField = overallField,
            )
        } else {
            return listOf(
                NadelResultInstruction.AddError(
                    NadelPartitionGraphQLErrorException(
                        "The call for field '${overallField.resultKey}' was not partitioned because the field type is " +
                            "not supported. The types supported are lists and mutation payloads.",
                        path = overallField.queryPath.segments,
                    )
                ),
                nullifyField
            )
        }

        val errorInstructions = resultFromPartitionCalls
            .flatMap { it.errors }
            .map { error ->
                NadelResultInstruction.AddError(
                    toGraphQLError(
                        error,
                        extensions = (error["extensions"] as? Map<String, Any> ?: emptyMap()).let {
                            it + mapOf("errorHappenedOnPartitionedCall" to true)
                        },
                        // TODO: Add location?
                    )
                )
            }

        return mergedData + errorInstructions
    }

    private fun mergeDataFromList(
        dataFromPartitionCalls: List<Any?>,
        thisNodesData: Any?,
        parentNodes: List<JsonNode>,
        overallField: ExecutableNormalizedField,
    ): List<NadelResultInstruction.Set> {
        val parentNode = parentNodes.first()

        val listDataFromPartitionCalls = dataFromPartitionCalls
            .mapNotNull {
                if (it == null) {
                    null
                } else {
                    check(it is List<*>) { "Expected a list, but got ${it::class.simpleName}" }
                    it
                }
            }.flatten()

        val thisNodesDataCast = thisNodesData?.let {
            check(it is List<*>) { "Expected a list, but got ${it::class.simpleName}" }
            it
        } ?: emptyList<NadelResultInstruction.Set>()

        return listOf(
            NadelResultInstruction.Set(
                subject = parentNode,
                newValue = JsonNode(thisNodesDataCast + listDataFromPartitionCalls),
                field = overallField
            )
        )
    }

    private fun mergeDataFromMutationPayloadLike(
        dataFromPartitionCalls: List<Any?>,
        thisNodesData: Any?,
        parentNodes: List<JsonNode>,
        overallField: ExecutableNormalizedField,
    ): List<NadelResultInstruction.Set> {
        val parentNode = parentNodes.first()

        // TODO: I'm not sure what's the best way to handle non-successful calls
        // - force `success` to be false?
        // - leave `success` alone?
        val nonSuccessPayload = mapOf("success" to false)

        val mutationPayloadLikeDataFromPartitionCalls = dataFromPartitionCalls
            .mapNotNull {
                if (it == null) {
                    nonSuccessPayload
                } else {
                    check(it is Map<*, *>) { "Expected a Map, but got ${it::class.simpleName}" }
                    it
                }
            }

        val thisNodesDataCast = thisNodesData?.let {
            check(it is Map<*, *>) { "Expected a Map, but got ${it::class.simpleName}" }
            it
        } ?: nonSuccessPayload

        val allListKeys = (thisNodesDataCast.keys + mutationPayloadLikeDataFromPartitionCalls.flatMap { it.keys })
            .distinct()

        val mergedData = mutationPayloadLikeDataFromPartitionCalls.fold(thisNodesDataCast) { acc, next ->
            allListKeys.associate {
                if (it == "success") {
                    Pair(it, acc[it].safeToBoolean() && next[it].safeToBoolean())
                } else {
                    if(acc[it] == null && next[it] == null) {
                        Pair(it, null)
                    } else {
                        Pair(it, acc[it].safeToList() + next[it].safeToList())
                    }
                }
            }
        }

        return listOf(
            NadelResultInstruction.Set(
                subject = parentNode,
                newValue = JsonNode(mergedData),
                field = overallField
            )
        )
    }
}

fun Any?.safeToBoolean(): Boolean {
    return (this as? Boolean) ?: false
}

fun Any?.safeToList(): List<Any> {
    return (this as? List<Any>) ?: emptyList()
}

/**
 * A GraphQL type is considered to be a "mutation payload" (for the purposes of this transform)  if
 * it has a `success` field of type `Boolean!` plus an `errors` field of type List and any other number
 * of fields of type List
 */
fun GraphQLOutputType.isMutationPayloadLike(): Boolean {
    return this is GraphQLObjectType
        && this.getField("success")?.let { it.type.unwrapNonNull() as? GraphQLScalarType }?.name == "Boolean"
        && this.getField("errors") != null
        && this.fields.filter { it.name != "success" }.all { it.type.unwrapNonNull() is GraphQLList }
}
