package graphql.nadel.tests.hooks

import graphql.nadel.NadelEngineContext
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformState
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.normalized.ExecutableNormalizedField

@UseHook
class `skip-include-does-not-affect-other-transforms` : EngineTestHook {
    object State : NadelTransformState

    override val customTransforms: List<NadelTransform<out NadelTransformState>>
        get() = listOf(
            object : NadelTransform<State> {
                context(NadelEngineContext, NadelExecutionContext)
                override suspend fun isApplicable(
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    hydrationDetails: ServiceExecutionHydrationDetails?,
                ): State? {
                    // All fields exist, no __skip field from NadelSkipIncludeTransform
                    assert(overallField.name != "__skip")
                    assert(overallField.getFieldDefinitions(executionBlueprint.engineSchema).isNotEmpty())

                    return null
                }

                context(NadelEngineContext, NadelExecutionContext, State)
                override suspend fun transformField(
                    transformer: NadelQueryTransformer,
                    service: Service,
                    field: ExecutableNormalizedField,
                ): NadelTransformFieldResult {
                    throw UnsupportedOperationException("Should never be invoked")
                }

                context(NadelEngineContext, NadelExecutionContext, State)
                override suspend fun getResultInstructions(
                    service: Service,
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
