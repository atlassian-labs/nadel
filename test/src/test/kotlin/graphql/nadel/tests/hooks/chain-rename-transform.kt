package graphql.nadel.tests.hooks

import graphql.language.EnumValue
import graphql.language.StringValue
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
import graphql.nadel.tests.hooks.ChainRenameTransform.TransformFieldContext
import graphql.nadel.tests.hooks.ChainRenameTransform.TransformOperationContext
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue

private class ChainRenameTransform : NadelTransform<TransformOperationContext, TransformFieldContext> {
    data class TransformOperationContext(
        override val parentContext: NadelOperationExecutionContext,
    ) : NadelTransformOperationContext()

    data class TransformFieldContext(
        override val parentContext: TransformOperationContext,
        override val overallField: ExecutableNormalizedField,
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
        return if (overallField.name == "test" || overallField.name == "cities") {
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
        if (field.normalizedArguments["arg"] != null) {
            return NadelTransformFieldResult(
                newField = field.toBuilder()
                    .normalizedArguments(field.normalizedArguments.let {
                        it + ("arg" to NormalizedInputValue("String", StringValue("aaarrg")))
                    })
                    .build(),
            )
        }

        if (field.normalizedArguments["continent"] != null) {
            return NadelTransformFieldResult(
                newField = field.toBuilder()
                    .normalizedArguments(field.normalizedArguments.let {
                        it + ("continent" to NormalizedInputValue("Continent", EnumValue("Asia")))
                    })
                    .build(),
            )
        }

        error("Did not match transform conditions")
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
class `chain-rename-transform` : EngineTestHook {
    override val customTransforms: List<NadelTransform<*, *>> = listOf(
        ChainRenameTransform(),
    )
}

@UseHook
class `chain-rename-transform-with-type-rename` : EngineTestHook {
    override val customTransforms: List<NadelTransform<*, *>> = listOf(
        ChainRenameTransform(),
    )
}
