package graphql.nadel.enginekt.transform

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.transform.NadelRenameArgumentInputTypesTransform.State
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNodes
import graphql.nadel.enginekt.util.toBuilder
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
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): State? {

        var changeCount = 0
        val newFieldArgs: Map<String, NormalizedInputValue> = overallField.normalizedArguments
            .mapValues { (_, inputValue) ->
                val newInputValue = renameInputValueType(inputValue, executionBlueprint, service)
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
        executionBlueprint: NadelOverallExecutionBlueprint,
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

    private fun renameInputValueType(
        inputValue: NormalizedInputValue,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
    ): NormalizedInputValue {
        val overallTypeName = inputValue.typeName
        val underlyingTypeName = executionBlueprint.getUnderlyingTypeName(service, overallTypeName)
        if (underlyingTypeName != overallTypeName) {
            return NormalizedInputValue(underlyingTypeName, inputValue.value)
        }
        return inputValue
    }
}

