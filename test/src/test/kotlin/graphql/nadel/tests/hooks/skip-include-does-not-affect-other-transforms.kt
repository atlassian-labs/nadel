package graphql.nadel.tests.hooks

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

@UseHook
class `skip-include-does-not-affect-other-transforms` : EngineTestHook {
    object TransformContext : NadelTransformContext

    override val customTransforms: List<NadelTransform<out NadelTransformContext>>
        get() = listOf(
            object : NadelTransform<TransformContext> {
                context(NadelEngineContext, NadelExecutionContext)
                override suspend fun isApplicable(
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    hydrationDetails: ServiceExecutionHydrationDetails?,
                ): TransformContext? {
                    // All fields exist, no __skip field from NadelSkipIncludeTransform
                    assert(overallField.name != "__skip")
                    assert(overallField.getFieldDefinitions(executionBlueprint.engineSchema).isNotEmpty())

                    return null
                }

                context(NadelEngineContext, NadelExecutionContext, TransformContext)
                override suspend fun transformField(
                    transformer: NadelQueryTransformer,
                    field: ExecutableNormalizedField,
                ): NadelTransformFieldResult {
                    throw UnsupportedOperationException("Should never be invoked")
                }

                context(NadelEngineContext, NadelExecutionContext, TransformContext)
                override suspend fun getResultInstructions(
                    overallField: ExecutableNormalizedField,
                    underlyingParentField: ExecutableNormalizedField?,
                    result: ServiceExecutionResult,
                    nodes: JsonNodes,
                ): List<NadelResultInstruction> {
                    throw UnsupportedOperationException("Should never be invoked")
                }
            }
        )
}
