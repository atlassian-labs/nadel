package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.nadel.NadelExecutionHints
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.normalized.ExecutableNormalizedField

@UseHook
class `all-hydration-fields-are-seen-by-transformer` : EngineTestHook {
    private val isApplicable = mutableListOf<String>()
    private val transformField = mutableListOf<String>()
    private val getResultInstructions = mutableListOf<String>()

    override fun makeExecutionHints(builder: NadelExecutionHints.Builder): NadelExecutionHints.Builder {
        return builder
            .removeHydrationSpecificExecutionCode(true)
    }

    override val customTransforms: List<NadelTransform<out Any>>
        get() = listOf(
            object : NadelTransform<Unit> {
                override suspend fun isApplicable(
                    executionContext: NadelExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    services: Map<String, Service>,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    hydrationDetails: ServiceExecutionHydrationDetails?,
                ): Unit? {
                    isApplicable.add(overallField.resultKey)
                    return Unit
                }

                override suspend fun transformField(
                    executionContext: NadelExecutionContext,
                    transformer: NadelQueryTransformer,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    service: Service,
                    field: ExecutableNormalizedField,
                    state: Unit,
                ): NadelTransformFieldResult {
                    transformField.add(field.resultKey)
                    return NadelTransformFieldResult.unmodified(field)
                }

                override suspend fun getResultInstructions(
                    executionContext: NadelExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    underlyingParentField: ExecutableNormalizedField?,
                    result: ServiceExecutionResult,
                    state: Unit,
                    nodes: JsonNodes,
                ): List<NadelResultInstruction> {
                    getResultInstructions.add(overallField.resultKey)
                    return emptyList()
                }
            },
        )

    override fun assertResult(result: ExecutionResult) {
        assert(isApplicable == listOf("foo", "bar", "name", "bars", "barById", "name"))
        // // name is missing because it's removed as part of the hydration query
        assert(transformField == listOf("foo", "bar", "bars", "barById", "name"))
        assert(getResultInstructions == listOf("foo", "bar", "bars", "barById", "name"))
    }
}
