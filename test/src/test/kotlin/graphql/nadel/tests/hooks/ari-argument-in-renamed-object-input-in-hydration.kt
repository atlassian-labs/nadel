package graphql.nadel.tests.hooks

import graphql.language.ArrayValue.newArrayValue
import graphql.language.ObjectField.newObjectField
import graphql.language.ObjectValue
import graphql.language.ObjectValue.newObjectValue
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
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.queryPath
import graphql.nadel.engine.util.toBuilder
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.schema.GraphQLAppliedDirective

@UseHook
class `ari-argument-in-renamed-object-input-in-hydration` : EngineTestHook {
    // Add hardcoded transforms to change arguments and result resource IDs to ARIs etc.
    override val customTransforms: List<NadelTransform<out Any>>
        get() = listOf(
            // Transforms arguments in IssueInput
            object : NadelTransform<Any> {
                override suspend fun isApplicable(
                    executionContext: NadelExecutionContext,
                    serviceExecutionContext: NadelServiceExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    services: Map<String, Service>,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    hydrationDetails: ServiceExecutionHydrationDetails?,
                ): Any? {
                    // Transforms arguments in IssueInput
                    return if (overallField.name == "issues") {
                        Unit
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
                ): NadelTransformFieldResult {
                    fun newInputObjectValue(projectId: String, issueId: String): ObjectValue {
                        return newObjectValue()
                            .objectField(
                                newObjectField()
                                    .name("projectId")
                                    .value(StringValue(projectId))
                                    .build(),
                            )
                            .objectField(
                                newObjectField()
                                    .name("issueId")
                                    .value(StringValue(issueId))
                                    .build(),
                            )
                            .build()
                    }

                    // Transforms arguments in IssueInput
                    return NadelTransformFieldResult(
                        newField = field.toBuilder()
                            .normalizedArguments(
                                mapOf(
                                    "input" to NormalizedInputValue(
                                        "[IssueInput]",
                                        newArrayValue()
                                            .value(newInputObjectValue(projectId = "100", issueId = "1"))
                                            .value(newInputObjectValue(projectId = "100", issueId = "2"))
                                            .value(newInputObjectValue(projectId = "101", issueId = "3"))
                                            .build(),
                                    ),
                                ),
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
                ): List<NadelResultInstruction> {
                    return emptyList()
                }
            },
            // Transforms result ids
            object : NadelTransform<GraphQLAppliedDirective> {
                override suspend fun isApplicable(
                    executionContext: NadelExecutionContext,
                    serviceExecutionContext: NadelServiceExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    services: Map<String, Service>,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    hydrationDetails: ServiceExecutionHydrationDetails?,
                ): GraphQLAppliedDirective? {
                    return overallField
                        .getFieldDefinitions(executionBlueprint.engineSchema)
                        .single()
                        .getAppliedDirective("ARI")
                }

                override suspend fun transformField(
                    executionContext: NadelExecutionContext,
                    serviceExecutionContext: NadelServiceExecutionContext,
                    transformer: NadelQueryTransformer,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    service: Service,
                    field: ExecutableNormalizedField,
                    state: GraphQLAppliedDirective,
                ): NadelTransformFieldResult {
                    return NadelTransformFieldResult.unmodified(field)
                }

                override suspend fun getResultInstructions(
                    executionContext: NadelExecutionContext,
                    serviceExecutionContext: NadelServiceExecutionContext,
                    executionBlueprint: NadelOverallExecutionBlueprint,
                    service: Service,
                    overallField: ExecutableNormalizedField,
                    underlyingParentField: ExecutableNormalizedField?,
                    result: ServiceExecutionResult,
                    state: GraphQLAppliedDirective,
                    nodes: JsonNodes,
                ): List<NadelResultInstruction> {
                    val parentNodes = nodes.getNodesAt(
                        underlyingParentField?.queryPath ?: NadelQueryPath.root,
                        flatten = true,
                    )

                    return parentNodes
                        .mapNotNull { parentNode ->
                            val parentNodeAsMap = parentNode.value as JsonMap?

                            if (parentNodeAsMap == null) {
                                null
                            } else {
                                val value = parentNodeAsMap[overallField.resultKey]
                                val type = (state.getArgument("type").argumentValue.value as StringValue).value
                                val owner = (state.getArgument("owner").argumentValue.value as StringValue).value

                                NadelResultInstruction.Set(
                                    subject = parentNode,
                                    key = NadelResultKey(overallField.resultKey),
                                    newValue = JsonNode("ari:cloud:$owner::$type/$value"),
                                )
                            }
                        }
                }
            }
        )

    override fun makeExecutionHints(builder: NadelExecutionHints.Builder): NadelExecutionHints.Builder {
        return builder
            .allDocumentVariablesHint { true }
    }
}
