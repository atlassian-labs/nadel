package graphql.nadel.engine.transform

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.transform.NadelRenameArgumentInputTypesTransform.State
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.replaceTypeName
import graphql.nadel.engine.util.toBuilder
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue

/**
 * This will rename a fields input arguments types.  This is especially important
 * when we use _$variable_ printing syntax and the printed document needs
 * to be in the underlying type names since the variable declarations
 * end up referencing the type names
 * ```
 * query x($var : UnderlyingTypeName!) { ... }
 * ```
 */
internal class NadelRenameArgumentInputTypesTransform : NadelTransform<State> {
    data class State(
        val newFieldArgs: Map<String, NormalizedInputValue>,
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): State? {

        var changeCount = 0
        val newFieldArgs: Map<String, NormalizedInputValue> = overallField.normalizedArguments
            .mapValues { (_, inputValue) ->
                val newInputValue = renameInputValueType(inputValue, service)
                if (newInputValue != inputValue) {
                    changeCount++
                }
                newInputValue
            }
        return if (changeCount == 0) {
            null
        } else {
            State(newFieldArgs)
        }
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformer,
        service: Service,
        field: ExecutableNormalizedField,
        state: State,
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult(
            newField = field.toBuilder().normalizedArguments(state.newFieldArgs).build()
        )
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: State,
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        return emptyList()
    }

    private fun renameInputValueType(
        inputValue: NormalizedInputValue,
        service: Service,
    ): NormalizedInputValue {
        val overallTypeName = inputValue.unwrappedTypeName
        val underlyingTypeName = service.blueprint.typeRenames.getUnderlyingName(overallTypeName)
        if (underlyingTypeName != overallTypeName) {
            //
            // in theory one could navigate down the `value` and if it's an object / list
            // we could rename the types inside it.  However, right now Nadel will never
            // address these inner NormalizedInputValue named types and hence for performance / complexity
            // reasons we don't do it.  Also, Nadel currently does not allow an input field to be renamed
            // and hence we don't have to change inner map keys either.
            //
            return inputValue.replaceTypeName(underlyingTypeName)
        }
        return inputValue
    }
}

