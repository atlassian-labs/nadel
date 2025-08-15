package graphql.nadel.engine.transform

import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.toBuilder
import graphql.nadel.engine.util.withNewUnwrappedTypeName
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue

data class NadelRenameArgumentInputTypesTransformOperationContext(
    override val parentContext: NadelOperationExecutionContext,
) : NadelTransformOperationContext()

data class NadelRenameArgumentInputTypesTransformFieldContext(
    override val parentContext: NadelRenameArgumentInputTypesTransformOperationContext,
    override val overallField: ExecutableNormalizedField,
) : NadelTransformFieldContext<NadelRenameArgumentInputTypesTransformOperationContext>()

/**
 * This will rename a fields input arguments types.  This is especially important
 * when we use _$variable_ printing syntax and the printed document needs
 * to be in the underlying type names since the variable declarations
 * end up referencing the type names
 *
 * ```graphql
 * query x($var : UnderlyingTypeName!) { ... }
 * ```
 */
internal class NadelRenameArgumentInputTypesTransform : NadelTransform<
    NadelRenameArgumentInputTypesTransformOperationContext,
    NadelRenameArgumentInputTypesTransformFieldContext
    > {
    override suspend fun getTransformOperationContext(
        operationExecutionContext: NadelOperationExecutionContext,
    ): NadelRenameArgumentInputTypesTransformOperationContext {
        return NadelRenameArgumentInputTypesTransformOperationContext(operationExecutionContext)
    }

    override suspend fun getTransformFieldContext(
        transformContext: NadelRenameArgumentInputTypesTransformOperationContext,
        overallField: ExecutableNormalizedField,
    ): NadelRenameArgumentInputTypesTransformFieldContext? {
        // Transform if there's any arguments at all
        // todo: this won't account for cases where a transform before this injected new arguments…
        // But that's not a big deal right now anyway…
        return if (overallField.normalizedArguments.isNotEmpty()) {
            NadelRenameArgumentInputTypesTransformFieldContext(transformContext, overallField)
        } else {
            null
        }
    }

    override suspend fun transformField(
        transformContext: NadelRenameArgumentInputTypesTransformFieldContext,
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult(
            newField = field
                .toBuilder()
                .normalizedArguments(getRenamedArguments(transformContext, field))
                .build(),
        )
    }

    override suspend fun transformResult(
        transformContext: NadelRenameArgumentInputTypesTransformFieldContext,
        underlyingParentField: ExecutableNormalizedField?,
        resultNodes: JsonNodes,
    ): List<NadelResultInstruction> {
        return emptyList()
    }

    private fun getRenamedArguments(
        transformContext: NadelRenameArgumentInputTypesTransformFieldContext,
        field: ExecutableNormalizedField,
    ): Map<String, NormalizedInputValue> {
        return field.normalizedArguments
            .mapValues { (_, inputValue) ->
                val overallTypeName = inputValue.unwrappedTypeName
                val underlyingTypeName = transformContext.executionBlueprint.getUnderlyingTypeName(
                    service = transformContext.service,
                    overallTypeName = overallTypeName,
                )

                if (overallTypeName == underlyingTypeName) {
                    inputValue
                } else {
                    inputValue.withNewUnwrappedTypeName(underlyingTypeName)
                }
            }
    }
}

