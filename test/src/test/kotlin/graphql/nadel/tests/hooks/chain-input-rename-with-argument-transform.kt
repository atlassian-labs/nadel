package graphql.nadel.tests.hooks

import graphql.language.NullValue
import graphql.language.StringValue
import graphql.nadel.NadelExecutionHints
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformServiceExecutionContext
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.toBuilder
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue

@UseHook
class `ari-argument-in-renamed-input` : EngineTestHook {
    override val customTransforms: List<NadelTransform<out Any>>
        get() = listOf(
            // This transform mimics the ARI transform
            object : NadelTransform<Any> {
                override suspend fun isApplicable(
                    executionContext: NadelExecutionContext,
                    serviceExecutionContext: NadelServiceExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    services: Map<String, Service>,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
                    hydrationDetails: ServiceExecutionHydrationDetails?,
                ): Any? {
                    return if (overallField.normalizedArguments.isNotEmpty()) {
                        overallField
                    } else {
                        null
                    }
                }

                override suspend fun transformField(
                    executionContext: NadelExecutionContext,
                    serviceExecutionContext: NadelServiceExecutionContext,
                    transformer: NadelQueryTransformer,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    service: Service,
                    field: ExecutableNormalizedField,
                    state: Any,
                    transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
                ): NadelTransformFieldResult {
                    return NadelTransformFieldResult(
                        newField = field.toBuilder()
                            .normalizedArguments(
                                field.normalizedArguments
                                    .mapValues { (_, value) ->
                                        require(value.typeName == "StartSprintInput")

                                        // Transforms the ARI to just resource ID - Hard coded
                                        NormalizedInputValue(
                                            "StartSprintInput",
                                            mapOf(
                                                "boardId" to NormalizedInputValue("ID!", StringValue("123")),
                                                "sprintId" to NormalizedInputValue("ID!", StringValue("456")),
                                                "name" to NormalizedInputValue("String!", StringValue("Test Input")),
                                                "goal" to NormalizedInputValue(
                                                    "String",
                                                    NullValue.newNullValue().build()
                                                ),
                                                "startDate" to NormalizedInputValue(
                                                    "String!",
                                                    StringValue("2022-03-22")
                                                ),
                                                "endDate" to NormalizedInputValue("String!", StringValue("2022-04-02")),
                                            ),
                                        )
                                    }
                            )
                            .build(),
                    )
                }

                override suspend fun getResultInstructions(
                    executionContext: NadelExecutionContext,
                    serviceExecutionContext: NadelServiceExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    underlyingParentField: ExecutableNormalizedField?,
                    result: ServiceExecutionResult,
                    state: Any,
                    nodes: JsonNodes,
                    transformServiceExecutionContext: NadelTransformServiceExecutionContext?,
                ): List<NadelResultInstruction> {
                    return emptyList()
                }
            },
        )

    override fun makeExecutionHints(builder: NadelExecutionHints.Builder): NadelExecutionHints.Builder {
        return builder
            .allDocumentVariablesHint { true }
    }
}
