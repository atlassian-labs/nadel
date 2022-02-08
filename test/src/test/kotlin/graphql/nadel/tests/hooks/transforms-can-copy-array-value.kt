package graphql.nadel.tests.hooks

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.enginekt.transform.NadelTransformFieldResult
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.transform.result.json.JsonNodes
import graphql.nadel.enginekt.util.queryPath
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.normalized.ExecutableNormalizedField

@UseHook
class `transforms-can-copy-array-value` : EngineTestHook {
    override val customTransforms: List<NadelTransform<out Any>>
        get() = listOf(
            object : NadelTransform<Any> {
                override suspend fun isApplicable(
                    executionContext: NadelExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    services: Map<String, Service>,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    hydrationDetails: ServiceExecutionHydrationDetails?,
                ): Any? {
                    return overallField.name.takeIf { it == "ids" }
                }

                override suspend fun transformField(
                    executionContext: NadelExecutionContext,
                    transformer: NadelQueryTransformer,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    service: Service,
                    field: ExecutableNormalizedField,
                    state: Any,
                ): NadelTransformFieldResult {
                    return NadelTransformFieldResult.unmodified(field)
                }

                override suspend fun getResultInstructions(
                    executionContext: NadelExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    underlyingParentField: ExecutableNormalizedField?,
                    result: ServiceExecutionResult,
                    state: Any,
                    nodes: JsonNodes,
                ): List<NadelResultInstruction> {
                    val nodes = JsonNodeExtractor.getNodesAt(
                        data = result.data,
                        queryPath = underlyingParentField!!.queryPath + overallField.resultKey,
                        flatten = true,
                    )

                    return nodes
                        .mapIndexed { index, node ->
                            val copyFromIndex = (index - 1)
                                .takeIf { it >= 0 }
                                ?: nodes.lastIndex

                            NadelResultInstruction.Copy(
                                subjectPath = node.resultPath.dropLast(1) + copyFromIndex,
                                destinationPath = node.resultPath
                            )
                        }
                        .onEach {
                            // Ensure we are operating an array elements
                            require(it.subjectPath.segments.last().value is Int)
                            require(it.destinationPath.segments.last().value is Int)
                        }
                }
            }
        )
}
