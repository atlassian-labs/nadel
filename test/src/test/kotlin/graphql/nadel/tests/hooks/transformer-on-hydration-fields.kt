package graphql.nadel.tests.hooks

import graphql.language.StringValue
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.enginekt.transform.NadelTransformFieldResult
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNodes
import graphql.nadel.enginekt.util.toBuilder
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue

@UseHook
class `transformer-on-hydration-fields` : EngineTestHook {

    private fun hasParentWithName(field: ExecutableNormalizedField, parentName: String): Boolean {
        return if (field.parent == null) {
            false
        } else if (field.parent.name == parentName) {
            true
        } else hasParentWithName(field.parent, parentName)
    }

    override val customTransforms: List<NadelTransform<out Any>>
        get() = listOf(
            /**
             * This transform will modify the arguments of the "barById" field.
             *
             * It will force a new value for the "id" argument, so we can assert that the transform was
             * executed in the test fixture.
             */
            object : NadelTransform<Any> {
                override suspend fun isApplicable(
                    executionContext: NadelExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    services: Map<String, Service>,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    hydrationDetails: ServiceExecutionHydrationDetails?,
                ): Any? {
                    return if (overallField.name == "barById") {
                        assert(hydrationDetails != null)
                        overallField
                    } else if (hasParentWithName(overallField, "barById")) {
                        assert(hydrationDetails != null)
                        null
                    } else {
                        assert(hydrationDetails == null)
                        null
                    }
                }

                override suspend fun transformField(
                    executionContext: NadelExecutionContext,
                    transformer: NadelQueryTransformer,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    service: Service,
                    field: ExecutableNormalizedField,
                    state: Any,
                ): NadelTransformFieldResult {
                    val transformedArgs = mapOf("id" to NormalizedInputValue("String", StringValue("transformed-id")))
                    return transformer.transform(field.children)
                        .let {
                            field.toBuilder()
                                .normalizedArguments(transformedArgs)
                                .build()
                        }.let {
                            NadelTransformFieldResult(
                                it,
                                emptyList()
                            )
                        }
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
                    return emptyList()
                }
            }
        )
}
