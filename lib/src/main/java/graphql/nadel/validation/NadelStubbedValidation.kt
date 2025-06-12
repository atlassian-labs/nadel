package graphql.nadel.validation

import graphql.nadel.definition.stubbed.NadelStubbedDefinition
import graphql.nadel.engine.blueprint.NadelStubbedInstruction
import graphql.nadel.engine.util.isNonNull
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.engine.util.whenType
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLObjectType

internal class NadelStubbedValidation {
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
     * @return null if field is not stubbed, otherwise return stub validation result
     */
    context(NadelValidationContext)
    fun validateOrNull(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult? {
        return if (isStubbed(parent, overallField)) {
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
            return NadelStubbedMustBeNullableError(parent, overallField)
        }

        if (parent is NadelServiceSchemaElement.Interface) {
            return validateInterfaceStub(parent, overallField.name)
        }

        return NadelValidatedFieldResult(
            service = parent.service,
            fieldInstruction = NadelStubbedInstruction(
                location = makeFieldCoordinates(parent.overall, overallField),
            ),
        )
    }

    context(NadelValidationContext)
    private fun validateInterfaceStub(
        interfaceType: NadelServiceSchemaElement.Interface,
        fieldName: String,
    ): NadelSchemaValidationResult {
        return engineSchema.getImplementations(interfaceType.overall)
            .asSequence()
            .map { objectType ->
                val objectField = objectType.getField(fieldName)!!
                if (isStubbed(objectType, objectField)) {
                    ok()
                } else {
                    NadelStubbedMissingOnConcreteType(interfaceType, objectType, objectField)
                }
            }
            .toResult()
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
        return instructionDefinitions.hasInstructionsOtherThan<NadelStubbedDefinition>(parent, overallField)
    }

    context(NadelValidationContext)
    private fun isStubbed(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallField: GraphQLFieldDefinition,
    ): Boolean {
        return isStubbed(parent.overall, overallField)
    }

    context(NadelValidationContext)
    private fun isStubbed(
        parent: GraphQLFieldsContainer,
        overallField: GraphQLFieldDefinition,
    ): Boolean {
        return instructionDefinitions.isStubbed(parent, overallField) || isOutputTypeStubbed(overallField)
    }
}
