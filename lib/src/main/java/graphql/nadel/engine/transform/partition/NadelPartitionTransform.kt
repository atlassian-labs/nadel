package graphql.nadel.engine.transform.partition

import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.blueprint.NadelPartitionInstruction
import graphql.nadel.engine.blueprint.getTypeNameToInstructionMap
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
import graphql.normalized.ExecutableNormalizedField
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
        val error: Throwable? = null,
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

        val partitionInstructions = executionBlueprint.fieldInstructions
            .getTypeNameToInstructionMap<NadelPartitionInstruction>(overallField)

        // We can't partition a field that has multiple partition instructions in different types. But, since
        // @partition can only be applied to fields in operation and namespaced types, we don't have to worry abou
        // this case.
        if (partitionInstructions.size != 1) {
            return null
        }

        val pathToPartitionArg = partitionInstructions.values.single().pathToPartitionArg

        val fieldPartitionContext = partitionTransformHook.getFieldPartitionContext(
                executionContext,
                serviceExecutionContext,
                executionBlueprint,
                services,
                service,
                overallField,
                hydrationDetails,
            )
            ?: // A field without a partition context can't be partitioned
            return null

        val fieldPartition = NadelFieldPartition(
            pathToPartitionArg = pathToPartitionArg,
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
                error = exception
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

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        transformer: NadelQueryTransformer,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: State,
    ): NadelTransformFieldResult {

        if (state.error != null) {
            throw NadelPartitionGraphQLErrorException(
                "The call for field '${field.resultKey}' was not partitioned due to the following error: '${state.error.message}'",
                path = field.queryPath.segments,
            )
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

        return mergedData + errorInstructions
    }

}
