package graphql.nadel.tests.hooks

import graphql.language.ArrayValue.newArrayValue
import graphql.language.ObjectField.newObjectField
import graphql.language.ObjectValue
import graphql.language.ObjectValue.newObjectValue
import graphql.language.StringValue
import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldContext
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformOperationContext
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
    data class TransformOperationContext(
        override val parentContext: NadelOperationExecutionContext,
    ) : NadelTransformOperationContext()

    data class TransformFieldContext(
        override val parentContext: TransformOperationContext,
        override val overallField: ExecutableNormalizedField,
    ) : NadelTransformFieldContext<TransformOperationContext>()

    data class TransformFieldContext2(
        override val parentContext: TransformOperationContext,
        override val overallField: ExecutableNormalizedField,
        val appliedDirective: GraphQLAppliedDirective,
    ) : NadelTransformFieldContext<TransformOperationContext>()

    override val customTransforms: List<NadelTransform<*, *>>
        get() = listOf(
            // Transforms arguments in IssueInput
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
                    // Transforms arguments in IssueInput
                    return if (overallField.name == "issues") {
                        TransformFieldContext(transformContext, overallField)
                    } else {
                        null
                    }
                }

                override suspend fun transformField(
                    transformContext: TransformFieldContext,
                    transformer: NadelQueryTransformer,
                    field: ExecutableNormalizedField,
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

                override suspend fun transformResult(
                    transformContext: TransformFieldContext,
                    underlyingParentField: ExecutableNormalizedField?,
                    resultNodes: JsonNodes,
                ): List<NadelResultInstruction> {
                    return emptyList()
                }
            },
            // Transforms result ids
            object : NadelTransform<TransformOperationContext, TransformFieldContext2> {
                override suspend fun getTransformOperationContext(
                    operationExecutionContext: NadelOperationExecutionContext,
                ): TransformOperationContext {
                    return TransformOperationContext(operationExecutionContext)
                }

                override suspend fun getTransformFieldContext(
                    transformContext: TransformOperationContext,
                    overallField: ExecutableNormalizedField,
                ): TransformFieldContext2? {
                    val ariDirective = overallField
                        .getFieldDefinitions(transformContext.engineSchema)
                        .single()
                        .getAppliedDirective("ARI")
                        ?: return null

                    return TransformFieldContext2(
                        parentContext = transformContext,
                        overallField = overallField,
                        appliedDirective = ariDirective,
                    )
                }

                override suspend fun transformField(
                    transformContext: TransformFieldContext2,
                    transformer: NadelQueryTransformer,
                    field: ExecutableNormalizedField,
                ): NadelTransformFieldResult {
                    return NadelTransformFieldResult.unmodified(field)
                }

                override suspend fun transformResult(
                    transformContext: TransformFieldContext2,
                    underlyingParentField: ExecutableNormalizedField?,
                    resultNodes: JsonNodes,
                ): List<NadelResultInstruction> {
                    val overallField = transformContext.overallField
                    val appliedDirective = transformContext.appliedDirective

                    val parentNodes = resultNodes.getNodesAt(
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
                                val type =
                                    (appliedDirective.getArgument("type").argumentValue.value as StringValue).value
                                val owner =
                                    (appliedDirective.getArgument("owner").argumentValue.value as StringValue).value

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
