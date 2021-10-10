package graphql.nadel.tests.hooks

import graphql.language.StringValue
import graphql.nadel.NadelExecutionHints
import graphql.nadel.NadelExecutionInput
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.enginekt.transform.NadelTransformFieldResult
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.util.toBuilder
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.KeepHook
import graphql.nadel.tests.NadelEngineType
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue

abstract class TransformerOnHydrationFieldsHook : EngineTestHook {

    abstract fun isHintOn(): Boolean

    override fun makeExecutionInput(
        engineType: NadelEngineType,
        builder: NadelExecutionInput.Builder,
    ): NadelExecutionInput.Builder {
        val isHintOn = this.isHintOn()

        return builder.nadelExecutionHints(
            NadelExecutionHints.newHints()
                .transformsOnHydrationFields(isHintOn)
                .build()
        )
    }

    override val customTransforms: List<NadelTransform<out Any>>
        get() = listOf(
            object : NadelTransform<Any> {
                override suspend fun isApplicable(
                    executionContext: NadelExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    services: Map<String, Service>,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                ): Any? {
                    return overallField.name.takeIf { it == "barById" }
                }

                override suspend fun transformField(
                    executionContext: NadelExecutionContext,
                    transformer: NadelQueryTransformer.Continuation,
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
                ): List<NadelResultInstruction> {
                    return emptyList()
                }
            }
        )
}

@KeepHook
class `transformer-on-hydration-fields-hint-on` : TransformerOnHydrationFieldsHook() {
    override fun isHintOn() = true
}

@KeepHook
class `transformer-on-hydration-fields-hint-off` : TransformerOnHydrationFieldsHook() {
    override fun isHintOn() = false
}
