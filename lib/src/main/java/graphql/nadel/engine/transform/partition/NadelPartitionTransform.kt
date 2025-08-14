package graphql.nadel.engine.transform.partition

import graphql.nadel.NextgenEngine
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.blueprint.NadelPartitionInstruction
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldContext
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformOperationContext
import graphql.nadel.engine.transform.partition.NadelPartitionMutationPayloadMerger.isMutationPayloadLike
import graphql.nadel.engine.transform.partition.NadelPartitionTransform.TransformFieldContext
import graphql.nadel.engine.transform.partition.NadelPartitionTransform.TransformOperationContext
import graphql.nadel.engine.transform.query.NFUtil
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.fieldPath
import graphql.nadel.engine.util.getType
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.queryPath
import graphql.nadel.engine.util.toGraphQLError
import graphql.normalized.ExecutableNormalizedField
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

internal class NadelPartitionTransform(
    private val engine: NextgenEngine,
    private val partitionTransformHook: NadelPartitionTransformHook,
) : NadelTransform<TransformOperationContext, TransformFieldContext> {
    data class TransformOperationContext(
        override val parentContext: NadelOperationExecutionContext,
    ) : NadelTransformOperationContext()

    data class TransformFieldContext(
        override val parentContext: TransformOperationContext,
        override val overallField: ExecutableNormalizedField,
        val userPartitionContext: NadelPartitionFieldContext,
        val fieldPartitions: Map<String, ExecutableNormalizedField>?,
        val error: Throwable?,
    ) : NadelTransformFieldContext<TransformOperationContext>() {
        val partitionCalls: MutableList<Deferred<ServiceExecutionResult>> = mutableListOf()
    }

    override suspend fun getTransformOperationContext(
        operationExecutionContext: NadelOperationExecutionContext,
    ): TransformOperationContext {
        return TransformOperationContext(operationExecutionContext)
    }

    override suspend fun getTransformFieldContext(
        transformContext: TransformOperationContext,
        overallField: ExecutableNormalizedField,
    ): TransformFieldContext? {
        if (transformContext.executionContext.isPartitionedCall) {
            // We don't want to partition a call that is already partitioned
            return null
        }

        val partitionInstructions = transformContext.executionBlueprint
            .getTypeNameToInstructionMap<NadelPartitionInstruction>(overallField)

        // We can't partition a field that has multiple partition instructions in different types. But, since
        // @partition can only be applied to fields in operation and namespaced types, we don't have to worry about
        // this case.
        if (partitionInstructions.size != 1) {
            return null
        }

        val pathToPartitionArg = partitionInstructions.values.single().pathToPartitionArg

        val userPartitionContext = partitionTransformHook.getPartitionFieldContext(
            transformContext.operationExecutionContext,
            overallField,
        )

        if (userPartitionContext == null) {
            // A field without a partition context can't be partitioned
            return null
        }

        val fieldPartition = NadelFieldPartition(
            pathToPartitionArg = pathToPartitionArg,
            userPartitionContext = userPartitionContext,
            engineSchema = transformContext.engineSchema,
            partitionKeyExtractor = partitionTransformHook.getPartitionKeyExtractor()
        )

        val fieldPartitions = try {
            fieldPartition.createFieldPartitions(field = overallField)
        } catch (exception: Exception) {
            return TransformFieldContext(
                parentContext = transformContext,
                overallField = overallField,
                userPartitionContext = userPartitionContext,
                fieldPartitions = null,
                error = exception,
            )
        }

        if (fieldPartitions.size < 2) {
            // We can't partition a field that doesn't have at least two partitions
            return null
        }

        partitionTransformHook.onPartition(transformContext.operationExecutionContext, fieldPartitions)

        return TransformFieldContext(
            parentContext = transformContext,
            overallField = overallField,
            userPartitionContext = userPartitionContext,
            fieldPartitions = fieldPartitions,
            error = null,
        )
    }

    override suspend fun transformField(
        transformContext: TransformFieldContext,
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
    ): NadelTransformFieldResult {
        if (transformContext.error != null) {
            throw NadelPartitionGraphQLErrorException(
                "The call for field '${field.resultKey}' was not partitioned due to the following error: '${transformContext.error.message}'",
                path = field.queryPath.segments,
            )
        }

        val executionContext = transformContext.executionContext
        val fieldPartitions = transformContext.fieldPartitions!!

        val primaryPartition = fieldPartitions.values.first()
        val otherPartitions = fieldPartitions.values.drop(1)

        // TODO: throw error if operation is Subscription?
        val rootType = executionContext.query.operation.getType(transformContext.engineSchema)

        val partitionCalls = otherPartitions.map {
            executionContext.executionCoroutine.async {
                val topLevelField = NFUtil.createField(
                    schema = transformContext.engineSchema,
                    parentType = rootType,
                    queryPathToField = field.fieldPath,
                    aliasedPath = it.queryPath,
                    fieldArguments = it.normalizedArguments,
                    fieldChildren = it.children
                )

                engine.executePartitionedCall(topLevelField, transformContext.service, executionContext)
            }
        }

        transformContext.partitionCalls.addAll(partitionCalls)

        return NadelTransformFieldResult(newField = primaryPartition)
    }

    override suspend fun transformResult(
        transformContext: TransformFieldContext,
        underlyingParentField: ExecutableNormalizedField?,
        resultNodes: JsonNodes,
    ): List<NadelResultInstruction> {
        val parentNode = resultNodes.getNodesAt(
            queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root,
            flatten = true,
        ).singleOrNull() ?: return emptyList()

        val overallField = transformContext.overallField
        val executionBlueprint = transformContext.executionBlueprint

        val nullifyField = NadelResultInstruction.Set(
            subject = parentNode,
            key = NadelResultKey(overallField.resultKey),
            newValue = null
        )

        // TODO: handle HTTP errors
        val resultFromPartitionCalls = transformContext.partitionCalls.awaitAll()

        val thisNodesData = resultNodes.getNodesAt(queryPath = overallField.queryPath, flatten = false).let {
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
                parentNode = parentNode,
                overallField = overallField,
            )
        } else if (overallFieldType.isMutationPayloadLike()) {
            NadelPartitionMutationPayloadMerger.mergeDataFromMutationPayloadLike(
                dataFromPartitionCalls = dataFromPartitionCalls,
                thisNodesData = thisNodesData,
                parentNode = parentNode,
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
            .asSequence()
            .flatMap { it.errors }
            .filterNotNull()
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
            .toList()

        return mergedData + errorInstructions
    }
}
