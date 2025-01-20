package graphql.nadel.validation

import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldInputType
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingInputField
import graphql.schema.GraphQLInputObjectField

class NadelInputObjectValidation internal constructor(
    private val assignableTypeValidation: NadelAssignableTypeValidation,
) {
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
        val isTypeAssignable = assignableTypeValidation.isInputTypeAssignable(
            overallType = overallInputField.type,
            underlyingType = underlyingInputField.type
        )

        return if (isTypeAssignable) {
            ok()
        } else {
            IncompatibleFieldInputType(parent, overallInputField, underlyingInputField)
        }
    }
}
