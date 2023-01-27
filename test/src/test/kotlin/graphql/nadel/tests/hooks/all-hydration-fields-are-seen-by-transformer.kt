package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.nadel.NadelEngineContext
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformContext
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.normalized.ExecutableNormalizedField
import java.util.Collections.synchronizedSet

@UseHook
class `all-hydration-fields-are-seen-by-transformer` : EngineTestHook {
    private val isApplicable = synchronizedSet(mutableSetOf<String>())
    private val transformField = synchronizedSet(mutableSetOf<String>())
    private val getResultInstructions = synchronizedSet(mutableSetOf<String>())

    private data class TransformContext(val service: Service) : NadelTransformContext

    override val customTransforms: List<NadelTransform<out NadelTransformContext>>
        get() = listOf(
            object : NadelTransform<TransformContext> {
                context(NadelEngineContext, NadelExecutionContext)
                override suspend fun isApplicable(
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    hydrationDetails: ServiceExecutionHydrationDetails?,
                ): TransformContext {
                    isApplicable.add("${service.name}.${overallField.resultKey}")
                    return TransformContext(service)
                }

                context(NadelEngineContext, NadelExecutionContext, TransformContext)
                override suspend fun transformField(
                    transformer: NadelQueryTransformer,
                    field: ExecutableNormalizedField,
                ): NadelTransformFieldResult {
                    transformField.add("${service.name}.${field.resultKey}")
                    return NadelTransformFieldResult.unmodified(field)
                }

                context(NadelEngineContext, NadelExecutionContext, TransformContext)
                override suspend fun getResultInstructions(
                    overallField: ExecutableNormalizedField,
                    underlyingParentField: ExecutableNormalizedField?,
                    result: ServiceExecutionResult,
                    nodes: JsonNodes,
                ): List<NadelResultInstruction> {
                    getResultInstructions.add("${service.name}.${overallField.resultKey}")
                    return emptyList()
                }
            },
        )

    override fun assertResult(result: ExecutionResult) {
        assert(
            isApplicable == setOf(
                "service1.foo",
                "service1.bar",
                "service1.name",
                "service2.bars",
                "service2.barById",
                "service2.name",
            )
        )
        // service1.name is missing because it's removed as part of the hydration query
        assert(
            transformField == setOf(
                "service1.foo",
                "service1.bar",
                "service2.bars",
                "service2.barById",
                "service2.name",
            )
        )
        assert(
            getResultInstructions == setOf(
                "service1.foo",
                "service1.bar",
                "service2.bars",
                "service2.barById",
                "service2.name",
            )
        )
    }
}
