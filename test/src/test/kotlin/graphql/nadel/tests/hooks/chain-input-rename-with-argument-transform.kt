package graphql.nadel.tests.hooks

import graphql.language.NullValue
import graphql.language.StringValue
import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldContext
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformOperationContext
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
    data class TransformOperationContext(
        override val parentContext: NadelOperationExecutionContext,
    ) : NadelTransformOperationContext()

    data class TransformFieldContext(
        override val parentContext: TransformOperationContext,
        override val overallField: ExecutableNormalizedField,
    ) : NadelTransformFieldContext<TransformOperationContext>()

    override val customTransforms: List<NadelTransform<*, *>>
        get() = listOf(
            // This transform mimics the ARI transform
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
                    return if (overallField.normalizedArguments.isNotEmpty()) {
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

                override suspend fun transformResult(
                    transformContext: TransformFieldContext,
                    underlyingParentField: ExecutableNormalizedField?,
                    resultNodes: JsonNodes,
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
