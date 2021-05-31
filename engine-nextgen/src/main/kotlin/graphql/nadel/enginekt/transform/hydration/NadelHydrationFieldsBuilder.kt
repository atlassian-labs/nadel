package graphql.nadel.enginekt.transform.hydration

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationArgumentValueSource
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.query.NadelPathToField
import graphql.normalized.NormalizedField
import graphql.schema.FieldCoordinates

internal object NadelHydrationFieldsBuilder {
    fun getExtraFields(
        service: Service,
        executionPlan: NadelExecutionPlan,
        fieldCoordinates: FieldCoordinates,
        instruction: NadelHydrationFieldInstruction,
    ): List<NormalizedField> {
        val underlyingTypeName = executionPlan.getUnderlyingTypeName(overallTypeName = fieldCoordinates.typeName)
        val underlyingObjectType = service.underlyingSchema.getObjectType(underlyingTypeName)
            ?: error("No underlying object type")

        return instruction.arguments
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
