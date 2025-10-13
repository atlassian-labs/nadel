package graphql.nadel.tests.hooks

import graphql.language.StringValue
import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldContext
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformOperationContext
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.getField
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.engine.util.toBuilder
import graphql.nadel.engine.util.toMapStrictly
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.hooks.AriTestTransform.TransformFieldContext
import graphql.nadel.tests.hooks.AriTestTransform.TransformOperationContext
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue

private class AriTestTransform : NadelTransform<TransformOperationContext, TransformFieldContext> {
    data class TransformOperationContext(
        override val parentContext: NadelOperationExecutionContext,
    ) : NadelTransformOperationContext()

    data class TransformFieldContext(
        override val parentContext: TransformOperationContext,
        override val overallField: ExecutableNormalizedField,
        val argumentNames: Set<String>,
    ) : NadelTransformFieldContext<TransformOperationContext>()

    override suspend fun getTransformOperationContext(
        operationExecutionContext: NadelOperationExecutionContext,
    ): TransformOperationContext {
        return TransformOperationContext(operationExecutionContext)
    }

    override suspend fun getTransformFieldContext(
        transformContext: TransformOperationContext,
        overallField: ExecutableNormalizedField,
    ): TransformFieldContext? {
        // Let's not bother with abstract types in a test
        val fieldCoords = makeFieldCoordinates(overallField.objectTypeNames.single(), overallField.name)
        val fieldDef = transformContext.executionBlueprint.engineSchema.getField(fieldCoords)
            ?: error("Unable to fetch field definition")
        val fieldArgDefs = fieldDef.arguments.strictAssociateBy { it.name }

        val fieldArgNames = overallField.normalizedArguments.keys
            .asSequence()
            .filter {
                fieldArgDefs[it]?.hasDirective("interpretAri") == true
            }
            .toSet()
            .takeIf {
                it.isNotEmpty()
            }
            ?: return null

        return TransformFieldContext(
            transformContext,
            overallField,
            fieldArgNames,
        )
    }

    override suspend fun transformField(
        transformContext: TransformFieldContext,
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
    ): NadelTransformFieldResult {
        val fieldsArgsToInterpret = transformContext.argumentNames

        return NadelTransformFieldResult(
            newField = field.toBuilder()
                .normalizedArguments(
                    field.normalizedArguments
                        .asSequence()
                        .map { (key, value) ->
                            if (key in fieldsArgsToInterpret) {
                                key to NormalizedInputValue(
                                    value.typeName,
                                    StringValue(
                                        // Strips the ari:/… prefix
                                        (value.value as StringValue).value.substringAfterLast("/"),
                                    ),
                                )
                            } else {
                                key to value
                            }
                        }
                        .toMapStrictly()
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
}

@UseHook
class `ari-argument-transform` : EngineTestHook {
    override val customTransforms: List<NadelTransform<*, *>> = listOf(
        AriTestTransform(),
    )
}

@UseHook
class `ari-argument-transform-on-renamed-field` : EngineTestHook {
    override val customTransforms: List<NadelTransform<*, *>> = listOf(
        AriTestTransform(),
    )
}
