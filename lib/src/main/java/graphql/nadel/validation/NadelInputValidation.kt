package graphql.nadel.validation

import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleArgumentInputType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldInputType
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingInputField
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLUnmodifiedType

class NadelInputValidation internal constructor() {
    private val typeWrappingValidation = NadelTypeWrappingValidation()

    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement.InputObject,
    ): NadelSchemaValidationResult {
        return validate(
            parent = schemaElement,
            overallFields = schemaElement.overall.fields,
            underlyingFields = schemaElement.underlying.fields,
        )
    }

    context(NadelValidationContext)
    private fun validate(
        parent: NadelServiceSchemaElement.InputObject,
        overallFields: List<GraphQLInputObjectField>,
        underlyingFields: List<GraphQLInputObjectField>,
    ): NadelSchemaValidationResult {
        val underlyingFieldsByName = underlyingFields.strictAssociateBy { it.name }

        return overallFields
            .map { overallField ->
                validate(parent, overallField, underlyingFieldsByName)
            }
            .toResult()
    }

    context(NadelValidationContext)
    private fun validate(
        parent: NadelServiceSchemaElement.InputObject,
        overallInputField: GraphQLInputObjectField,
        underlyingFieldsByName: Map<String, GraphQLInputObjectField>,
    ): NadelSchemaValidationResult {
        val underlyingInputField = underlyingFieldsByName[overallInputField.name]

        return if (underlyingInputField == null) {
            MissingUnderlyingInputField(parent, overallInputField)
        } else {
            validate(parent, overallInputField, underlyingInputField)
        }
    }

    context(NadelValidationContext)
    private fun validate(
        parent: NadelServiceSchemaElement.InputObject,
        overallInputField: GraphQLInputObjectField,
        underlyingInputField: GraphQLInputObjectField,
    ): NadelSchemaValidationResult {
        return if (!isInputTypeValid(overallInputField.type, underlyingInputField.type)) {
            IncompatibleFieldInputType(parent, overallInputField, underlyingInputField)
        } else {
            ok()
        }
    }

    context(NadelValidationContext)
    fun validate(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallField: GraphQLFieldDefinition,
        overallInputArgument: GraphQLArgument,
        underlyingInputArgument: GraphQLArgument,
    ): NadelSchemaValidationResult {
        return if (!isInputTypeValid(overallInputArgument.type, underlyingInputArgument.type)) {
            IncompatibleArgumentInputType(parent, overallField, overallInputArgument, underlyingInputArgument)
        } else {
            ok()
        }
    }

    context(NadelValidationContext)
    private fun isInputTypeValid(
        overallType: GraphQLInputType,
        underlyingType: GraphQLInputType,
    ): Boolean {
        val typeWrappingValid = typeWrappingValidation.isTypeWrappingValid(
            lhs = overallType,
            rhs = underlyingType,
            rule = NadelTypeWrappingValidation.Rule.LHS_MUST_BE_STRICTER_OR_SAME,
        )

        return typeWrappingValid && isInputTypeNameValid(overallType.unwrapAll(), underlyingType.unwrapAll())
    }

    context(NadelValidationContext)
    private fun isInputTypeNameValid(
        overallType: GraphQLUnmodifiedType,
        underlyingType: GraphQLUnmodifiedType,
    ): Boolean {
        return getUnderlyingTypeName(overallType) == underlyingType.name
    }
}
