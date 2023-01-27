package graphql.nadel.tests.hooks

import graphql.language.NullValue
import graphql.language.StringValue
import graphql.nadel.NadelEngineContext
import graphql.nadel.NadelExecutionHints
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
import graphql.nadel.engine.util.toBuilder
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue

@UseHook
class `ari-argument-in-renamed-input` : EngineTestHook {
    private data class State(val field: ExecutableNormalizedField) : NadelTransformState

    override val customTransforms: List<NadelTransform<out NadelTransformState>>
        get() = listOf(
            // This transform mimics the ARI transform
            object : NadelTransform<State> {
                context(NadelEngineContext, NadelExecutionContext)
                override suspend fun isApplicable(
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    hydrationDetails: ServiceExecutionHydrationDetails?,
                ): State? {
                    return if (overallField.normalizedArguments.isNotEmpty()) {
                        State(overallField)
                    } else {
                        null
                    }
                }

                context(NadelEngineContext, NadelExecutionContext, State)
    override suspend fun transformField(
                    transformer: NadelQueryTransformer,
                    service: Service,
                    field: ExecutableNormalizedField,
                    state: State,
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

                context(NadelEngineContext, NadelExecutionContext, State)
    override suspend fun getResultInstructions(
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    underlyingParentField: ExecutableNormalizedField?,
                    result: ServiceExecutionResult,
                    state: State,
                    nodes: JsonNodes,
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
