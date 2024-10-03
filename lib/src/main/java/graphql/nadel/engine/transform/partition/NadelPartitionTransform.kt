package graphql.nadel.engine.transform.partition

import graphql.language.ArrayValue
import graphql.language.StringValue
import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.partition.NadelPartitionMutationPayloadMerger.isMutationPayloadLike
import graphql.nadel.engine.transform.query.NFUtil
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.getType
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.queryPath
import graphql.nadel.engine.util.toGraphQLError
import graphql.nadel.schema.NadelDirectives
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLSchema
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal class NadelPartitionTransform(
    private val engine: NextgenEngine,
    private val partitionTransformHook: NadelPartitionTransformHook,
) : NadelTransform<NadelPartitionTransform.State> {
    data class State(
        val executionContext: NadelExecutionContext,
        val fieldPartitionContext: Any,
        val fieldPartitions: Map<String, ExecutableNormalizedField>? = null,
        val partitionCalls: MutableList<Deferred<ServiceExecutionResult>> = mutableListOf(),
        val errors: List<Throwable>? = null,
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): State? {

        if (executionContext.isPartitionedCall) {
            // We don't want to partition a call that is already partitioned
            return null
        }

        // TODO: This could be in the blueprint
        val fieldPartitionContext = partitionTransformHook.getFieldPartitionContext(
                executionContext,
                serviceExecutionContext,
                executionBlueprint,
                services,
                service,
                overallField,
                hydrationDetails,
            )

        if (fieldPartitionContext == null) {
            // A field without a partition context can't be partitioned
            return null
        }

        val fieldPartition = NadelFieldPartition(
            pathToPartitionArg = getPathToPartitionArg(overallField, executionBlueprint.engineSchema) ?: return null,
            fieldPartitionContext = fieldPartitionContext,
            graphQLSchema = executionBlueprint.engineSchema,
            partitionKeyExtractor = partitionTransformHook.getPartitionKeyExtractor()
        )

        val fieldPartitions = try {
            fieldPartition.createFieldPartitions(field = overallField)
        } catch (exception: Exception) {
            return State(
                executionContext = executionContext,
                fieldPartitionContext = fieldPartitionContext,
                errors = mutableListOf(exception)
            )
        }

        if (fieldPartitions.size < 2) {
            // We can't partition a field that doesn't have at least two partitions
            return null
        }

        partitionTransformHook.willPartitionCallback(executionContext, fieldPartitions)

        return State(
            executionContext = executionContext,
            fieldPartitionContext = fieldPartitionContext,
            fieldPartitions = fieldPartitions
        )
    }

    private fun getPathToPartitionArg(overallField: ExecutableNormalizedField, schema: GraphQLSchema): List<String>?  {
        val fieldDef = overallField.getFieldDefinitions(schema).first()
        val routingDirective = fieldDef.getAppliedDirective(NadelDirectives.partitionDirectiveDefinition.name) ?: return null
        val pathToSplitPoint = routingDirective.getArgument("pathToSplitPoint")
            ?.argumentValue
            ?.value as ArrayValue?

        return pathToSplitPoint?.values?.map { it as StringValue }?.map { it.value }
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

        if (state.errors != null) {
            // At this point we know we can't partition the field, but removing it could result
            // in a query with no top-level fields, which would be invalid.
            // Adding __typename as an artificial field also wouldn't work because that field
            // belongs to all operation types, which causes issues further down the execution.
            return NadelTransformFieldResult.unmodified(field)
        }

        val fieldPartitions = checkNotNull(state.fieldPartitions) { "Expected fieldPartitions to be set" }

        val primaryPartition = fieldPartitions.values.first()

        // TODO: throw error if operation is Subscription?
        val rootType = executionContext.query.operation.getType(executionBlueprint.engineSchema)

        val partitionCalls = coroutineScope {
            fieldPartitions.values.drop(1).map {
                async {
                    val topLevelField = NFUtil.createField(
                        executionBlueprint.engineSchema,
                        rootType,
                        // TODO: queryPath contains field aliases, not field names, which results in an error.
                        field.queryPath,
                        it.normalizedArguments,
                        it.children
                    )

                    engine.executePartitionedCall(topLevelField, service, state.executionContext)
                }
            }
        }

        state.partitionCalls.addAll(partitionCalls)

        return NadelTransformFieldResult(newField = primaryPartition)
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
            // TODO: Support multiple parent nodes (interfaces, unions)
            return emptyList()
        }

        val nullifyField = NadelResultInstruction.Set(
            subject = parentNodes.first(),
            key = NadelResultKey(overallField.resultKey),
            newValue = null
        )

        if (state.errors != null) {
            return state.errors.map {
                NadelResultInstruction.AddError(
                    NadelPartitionGraphQLErrorException(
                        "The call for field '${overallField.resultKey}' was not partitioned due to the following error: '${it.message}'",
                        path = overallField.queryPath.segments,
                    )
                )
            } + nullifyField
        }

        // TODO: handle HTTP errors
        val resultFromPartitionCalls = state.partitionCalls.awaitAll()

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

        // TODO: how to merge data when other transforms have manipulated the nodes (renames, etc)?
        val mergedData = if (overallFieldType.isList) {
            NadelPartitionListMerger.mergeDataFromList(
                dataFromPartitionCalls = dataFromPartitionCalls,
                thisNodesData = thisNodesData,
                parentNodes = parentNodes,
                overallField = overallField,
            )
        } else if (overallFieldType.isMutationPayloadLike()) {
            NadelPartitionMutationPayloadMerger.mergeDataFromMutationPayloadLike(
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

        // TODO: Add "extensions" to the result?
        // {"partitionedCall": true, "partitionsCalled": ["cloudId-1", "cloudId-2"]}
        return mergedData + errorInstructions
    }

}
