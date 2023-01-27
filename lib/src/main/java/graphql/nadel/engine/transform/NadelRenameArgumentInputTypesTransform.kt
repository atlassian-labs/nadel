package graphql.nadel.engine.transform

import graphql.nadel.NadelEngineContext
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelRenameArgumentInputTypesTransform.TransformContext
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.toBuilder
import graphql.nadel.engine.util.withNewUnwrappedTypeName
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue

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
internal class NadelRenameArgumentInputTypesTransform : NadelTransform<TransformContext> {
    data class TransformContext(val service: Service) : NadelTransformContext

    context(NadelEngineContext, NadelExecutionContext)
    override suspend fun isApplicable(
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): TransformContext? {
        // Transform if there's any arguments at all
        // todo: this won't account for cases where a transform before this injected new arguments…
        // But that's not a big deal right now anyway…
        return if (overallField.normalizedArguments.isNotEmpty()) {
            TransformContext(service)
        } else {
            null
        }
    }

    context(NadelEngineContext, NadelExecutionContext, TransformContext)
    override suspend fun transformField(
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult(
            newField = field
                .toBuilder()
                .normalizedArguments(getRenamedArguments(executionBlueprint, service, field))
                .build(),
        )
    }

    context(NadelEngineContext, NadelExecutionContext, TransformContext)
    override suspend fun getResultInstructions(
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        return emptyList()
    }

    private fun getRenamedArguments(
        blueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
    ): Map<String, NormalizedInputValue> {
        return field.normalizedArguments
            .mapValues { (_, inputValue) ->
                val overallTypeName = inputValue.unwrappedTypeName
                val underlyingTypeName = blueprint.getUnderlyingTypeName(service, overallTypeName)

                if (overallTypeName == underlyingTypeName) {
                    inputValue
                } else {
                    inputValue.withNewUnwrappedTypeName(underlyingTypeName)
                }
            }
    }
}

