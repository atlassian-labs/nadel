package graphql.nadel.enginekt.transform.hydration

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationArgumentValueSource
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.query.NadelPathToField
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.schema.FieldCoordinates

internal object NadelHydrationFieldsBuilder {
    fun getQuery(
        instruction: NadelHydrationFieldInstruction,
        hydrationField: NormalizedField,
        parentNode: JsonNode,
        pathToResultKeys: (List<String>) -> List<String>,
    ): NormalizedField {
        return getQuery(
            instruction,
            hydrationField,
            fieldArguments = NadelHydrationArgumentsBuilder.createSourceFieldArgs(
                instruction,
                parentNode,
                hydrationField,
                pathToResultKeys,
            ),
        )
    }

    fun getQuery(
        instruction: NadelGenericHydrationInstruction,
        hydrationField: NormalizedField,
        fieldArguments: Map<String, NormalizedInputValue>,
    ): NormalizedField {
        return NadelPathToField.createField(
            schema = instruction.sourceService.underlyingSchema,
            parentType = instruction.sourceService.underlyingSchema.queryType,
            pathToField = instruction.pathToSourceField,
            fieldArguments = fieldArguments,
            fieldChildren = hydrationField.children,
        )
    }

    fun getArtificialFields(
        service: Service,
        executionPlan: NadelExecutionPlan,
        fieldCoordinates: FieldCoordinates,
        instruction: NadelGenericHydrationInstruction,
    ): List<NormalizedField> {
        val underlyingTypeName = executionPlan.getUnderlyingTypeName(overallTypeName = fieldCoordinates.typeName)
        val underlyingObjectType = service.underlyingSchema.getObjectType(underlyingTypeName)
            ?: error("No underlying object type")

        return instruction.sourceFieldArguments
            .asSequence()
            .map { it.valueSource }
            .filterIsInstance<NadelHydrationArgumentValueSource.FieldValue>()
            .map { valueSource ->
                NadelPathToField.createField(
                    schema = service.underlyingSchema,
                    parentType = underlyingObjectType,
                    pathToField = valueSource.pathToField,
                    fieldArguments = emptyMap(),
                    fieldChildren = emptyList(), // This must be a leaf node
                )
            }
            .toList()
    }
}
