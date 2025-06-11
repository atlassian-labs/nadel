package graphql.nadel.validation

import graphql.nadel.definition.stubbed.NadelStubbedDefinition
import graphql.nadel.engine.blueprint.NadelStubbedInstruction
import graphql.nadel.engine.util.isNonNull
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.engine.util.whenType
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType

class NadelStubbedValidation {
    context(NadelValidationContext)
    fun validate(
        type: NadelServiceSchemaElement.StubbedType,
    ): NadelSchemaValidationResult {
        val objectType = type.overall as GraphQLObjectType // Must be object type
        if (objectType.interfaces.isNotEmpty()) {
            return NadelStubbedTypeMustNotImplementError(type)
        }

        return ok()
    }

    /**
     * @return null if field is not stubbed, stub validation result otherwise
     */
    context(NadelValidationContext)
    fun validateOrNull(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult? {
        return if (instructionDefinitions.isStubbed(parent, overallField)) {
            validateStubbedField(parent, overallField)
        } else if (isOutputTypeStubbed(overallField)) {
            validateStubbedField(parent, overallField)
        } else {
            null
        }
    }

    context(NadelValidationContext)
    private fun validateStubbedField(
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
    private fun isOutputTypeStubbed(field: GraphQLFieldDefinition): Boolean {
        return field.type.unwrapAll().whenType(
            enumType = { false },
            inputObjectType = { false },
            interfaceType = { false }, // Stubbed types cannot be part of hierarchies for now…
            objectType = instructionDefinitions::isStubbed,
            scalarType = { false },
            unionType = { false },
        )
    }

    context(NadelValidationContext)
    private fun hasIncompatibleInstructions(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallField: GraphQLFieldDefinition,
    ): Boolean {
        return instructionDefinitions.hasOtherInstructions<NadelStubbedDefinition>(parent, overallField)
    }
}
