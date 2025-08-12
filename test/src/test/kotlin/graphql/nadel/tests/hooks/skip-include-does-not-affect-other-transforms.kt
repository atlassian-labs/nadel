package graphql.nadel.tests.hooks

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
import kotlin.test.assertTrue

@UseHook
class `skip-include-does-not-affect-other-transforms` : EngineTestHook {
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
                    // All fields exist, no __skip field from NadelSkipIncludeTransform
                    assertTrue(overallField.name != "__skip")
                    assertTrue(overallField.getFieldDefinitions(transformContext.engineSchema).isNotEmpty())

                    return TransformFieldContext(transformContext, overallField)
                }

                override suspend fun transformField(
                    transformContext: TransformFieldContext,
                    transformer: NadelQueryTransformer,
                    field: ExecutableNormalizedField,
                ): NadelTransformFieldResult {
                    assertTrue(field.name != "__skip")

                    return NadelTransformFieldResult.unmodified(field)
                }

                override suspend fun transformResult(
                    transformContext: TransformFieldContext,
                    underlyingParentField: ExecutableNormalizedField?,
                    resultNodes: JsonNodes,
                ): List<NadelResultInstruction> {
                    assertTrue(transformContext.overallField.name != "__skip")

                    return emptyList()
                }
            }
        )
}
