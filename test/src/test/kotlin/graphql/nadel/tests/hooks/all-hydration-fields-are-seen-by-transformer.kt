package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldContext
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformOperationContext
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

    data class TransformOperationContext(
        override val parentContext: NadelOperationExecutionContext,
    ) : NadelTransformOperationContext()

    data class TransformFieldContext(
        override val parentContext: TransformOperationContext,
        override val overallField: ExecutableNormalizedField,
    ) : NadelTransformFieldContext<TransformOperationContext>()

    override val customTransforms: List<NadelTransform<*, *>>
        get() = listOf(
            object : NadelTransform<TransformOperationContext, TransformFieldContext> {
                override suspend fun getTransformOperationContext(
                    operationExecutionContext: NadelOperationExecutionContext,
                ): TransformOperationContext {
                    return TransformOperationContext(operationExecutionContext)
                }

                override suspend fun getTransformFieldContext(
                    transformContext: TransformOperationContext,
                    overallField: ExecutableNormalizedField,
                ): TransformFieldContext? {
                    isApplicable.add("${transformContext.service.name}.${overallField.resultKey}")
                    return TransformFieldContext(transformContext, overallField)
                }

                override suspend fun transformField(
                    transformContext: TransformFieldContext,
                    transformer: NadelQueryTransformer,
                    field: ExecutableNormalizedField,
                ): NadelTransformFieldResult {
                    transformField.add("${transformContext.service.name}.${field.resultKey}")
                    return NadelTransformFieldResult.unmodified(field)
                }

                override suspend fun transformResult(
                    transformContext: TransformFieldContext,
                    underlyingParentField: ExecutableNormalizedField?,
                    resultNodes: JsonNodes,
                ): List<NadelResultInstruction> {
                    getResultInstructions.add("${transformContext.service.name}.${transformContext.overallField.resultKey}")
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
