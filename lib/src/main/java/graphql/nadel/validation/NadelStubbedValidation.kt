package graphql.nadel.validation

import graphql.nadel.definition.stubbed.NadelStubbedDefinition
import graphql.nadel.engine.blueprint.NadelStubbedInstruction
import graphql.nadel.engine.util.isNonNull
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.engine.util.whenType
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLUnionType

internal class NadelStubbedValidation {
    context(NadelValidationContext)
    fun validate(
        type: NadelServiceSchemaElement.StubbedType,
    ): NadelSchemaValidationResult {
        return type.overall.whenType(
            enumType = {
                validateStubbedEnumType(type, it)
            },
            inputObjectType = {
                throw UnsupportedOperationException("Cannot stub InputObjectType")
            },
            interfaceType = {
                throw UnsupportedOperationException("Cannot stub InterfaceType")
            },
            objectType = {
                validateStubbedObjectType(type, it)
            },
            scalarType = {
                throw UnsupportedOperationException("Cannot stub ScalarType")
            },
            unionType = {
                validateStubbedUnionType(type, it)
            },
        )
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
    private fun validateStubbedEnumType(
        type: NadelServiceSchemaElement.StubbedType,
        enumType: GraphQLEnumType,
    ): NadelSchemaValidationResult {
        return ok()
    }

    context(NadelValidationContext)
    private fun validateStubbedUnionType(
        type: NadelServiceSchemaElement.StubbedType,
        objectType: GraphQLUnionType,
    ): NadelSchemaValidationResult {
        return ok()
    }

    context(NadelValidationContext)
    private fun validateStubbedObjectType(
        type: NadelServiceSchemaElement.StubbedType,
        objectType: GraphQLObjectType,
    ): NadelSchemaValidationResult {
        if (objectType.interfaces.isNotEmpty()) {
            return NadelStubbedTypeMustNotImplementError(type)
        }

        return ok()
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
                    NadelStubbedMissingOnConcreteTypeError(interfaceType, objectType, objectField)
                }
            }
            .toResult()
    }

    context(NadelValidationContext)
    private fun isOutputTypeStubbed(field: GraphQLFieldDefinition): Boolean {
        return field.type.unwrapAll().whenType(
            enumType = instructionDefinitions::isStubbed,
            inputObjectType = { false },
            interfaceType = { false }, // Stubbed types cannot be part of hierarchies for now…
            objectType = instructionDefinitions::isStubbed,
            scalarType = { false },
            unionType = instructionDefinitions::isStubbed,
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
