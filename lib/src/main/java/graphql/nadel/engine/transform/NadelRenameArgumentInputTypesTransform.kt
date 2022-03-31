package graphql.nadel.engine.transform

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.transform.NadelRenameArgumentInputTypesTransform.State
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
internal class NadelRenameArgumentInputTypesTransform : NadelTransform<State> {
    object State

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): State? {
        // Transform if there's any arguments at all
        // todo: this won't account for cases where a transform before this injected new arguments…
        // But that's not a big deal right now anyway…
        return if (overallField.normalizedArguments.isNotEmpty()) {
            State
        } else {
            null
        }
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformer,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: State,
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult(
            newField = field
                .toBuilder()
                .normalizedArguments(getRenamedArguments(executionBlueprint, service, field))
                .build(),
        )
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: State,
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

