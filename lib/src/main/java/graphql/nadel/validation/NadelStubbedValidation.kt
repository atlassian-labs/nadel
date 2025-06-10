package graphql.nadel.validation

import graphql.nadel.engine.blueprint.NadelStubbedInstruction
import graphql.nadel.engine.util.isNonNull
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.schema.GraphQLFieldDefinition

class NadelStubbedValidation {
    context(NadelValidationContext)
    fun validate(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult {
        if (hasIncompatibleInstructions(parent, overallField)) {
            return NadelStubbedMustBeUsedExclusively(parent, overallField)
        }

        if (overallField.type.isNonNull) {
            return NadelStubbedOnNonNullFieldError(parent, overallField)
        }

        return NadelValidatedFieldResult(
            service = parent.service,
            fieldInstruction = NadelStubbedInstruction(
                location = makeFieldCoordinates(parent.overall, overallField),
            ),
        )
    }

    context(NadelValidationContext)
    private fun hasIncompatibleInstructions(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallField: GraphQLFieldDefinition,
    ): Boolean {
        return instructionDefinitions.hasOtherInstructions(parent, overallField)
    }
}
