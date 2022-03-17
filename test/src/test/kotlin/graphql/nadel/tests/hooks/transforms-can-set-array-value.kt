package graphql.nadel.tests.hooks

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.queryPath
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.normalized.ExecutableNormalizedField

@UseHook
class `transforms-can-set-array-value` : EngineTestHook {
    override val customTransforms: List<NadelTransform<out Any>>
        get() = listOf(
            object : NadelTransform<Any> {
                override suspend fun isApplicable(
                    executionContext: NadelExecutionContext,
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
                    service: Service,
                    field: ExecutableNormalizedField,
                    state: Any,
                ): NadelTransformFieldResult {
                    return NadelTransformFieldResult.unmodified(field)
                }

                override suspend fun getResultInstructions(
                    executionContext: NadelExecutionContext,
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
                            NadelResultInstruction.Set(
                                subjectPath = node.resultPath,
                                newValue = "$index-${node.value}",
                            )
                        }
                        .onEach {
                            // Ensure we are setting an array element
                            require(it.subjectPath.segments.last().value is Int)
                        }
                }
            }
        )
}
