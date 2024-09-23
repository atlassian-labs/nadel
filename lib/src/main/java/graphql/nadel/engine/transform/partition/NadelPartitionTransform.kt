package graphql.nadel.engine.transform.partition

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
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.queryPath
import graphql.normalized.ExecutableNormalizedField
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
        val partitionCalls: MutableList<Deferred<ServiceExecutionResult>> = mutableListOf(),
        val pathToPartitionPoint: List<String>,
    )

    private val fieldPartition = FieldPartition(instructions.getPartitionKeyExtractor())

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        serviceExecutionContext: NadelServiceExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): State? {

        // TODO: surely there's a more idiomatic way to write this
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

        val fieldPartitions = fieldPartition.createFieldPartitions(
            field = field,
            pathToPartitionPoint = state.pathToPartitionPoint,
            graphQLSchema = executionBlueprint.engineSchema
        )

        val firstPartition = fieldPartitions.values.first()

        val partitionCalls = coroutineScope {
            fieldPartitions.values.drop(1).map {
                async {
                    val underlyingTypeName =
                        executionBlueprint.getUnderlyingTypeName(service, it.objectTypeNamesToString())
                    val underlyingObjectType = service.underlyingSchema.getObjectType(underlyingTypeName)
                        ?: error("No underlying object type")

                    val topLevelField = NFUtil.createField(
                        executionBlueprint.engineSchema,
                        // TODO: root can also be a mutation
                        executionBlueprint.engineSchema.queryType,
                        field.queryPath,
                        it.normalizedArguments,
                        it.children
                    )
                    // TODO: Maybe add a suffix to the operation name to identity this is a partitioned call
                    engine.executePartitionedCall(topLevelField, service, state.executionContext)
                }
            }
        }

        state.partitionCalls.addAll(partitionCalls)

        return NadelTransformFieldResult(
            newField = firstPartition
        )
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
        // TODO: handle HTTP errors
        val resultFromPartitionCalls = state.partitionCalls.awaitAll()

        val parentNodes = nodes.getNodesAt(
            queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root,
            flatten = true,
        )

        if (parentNodes.size != 1) {
            // TODO: Log strange log - should always be 1, right?
            return emptyList()
        }

        val parentNode = parentNodes.first()

        val thisNodesData = nodes.getNodesAt(queryPath = overallField.queryPath, flatten = true)
            .map { it.value }

        val dataFromPartitionCalls = resultFromPartitionCalls.flatMap { resultFromPartitionCall ->
            JsonNodes(resultFromPartitionCall.data).getNodesAt(overallField.queryPath, flatten = true)
                .map { node -> node.value }
        }

        if(thisNodesData is List<*>) {
            return listOf(
                NadelResultInstruction.Set(
                    subject = parentNode,
                    newValue = JsonNode(thisNodesData + dataFromPartitionCalls),
                    field = overallField
                )
            )
        }

        // TODO: handle other response shapes (MutationPayload, etc.)
        return emptyList()
    }
}
