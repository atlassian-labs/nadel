package graphql.nadel.validation

import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingInputField
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLInputObjectType

internal class NadelInputValidation {
    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationError> {
        return if (schemaElement.overall is GraphQLInputObjectType && schemaElement.underlying is GraphQLInputObjectType) {
            validate(
                parent = schemaElement,
                overallFields = schemaElement.overall.fields,
                underlyingFields = schemaElement.underlying.fields,
            )
        } else {
            emptyList()
        }
    }

    private fun validate(
        parent: NadelServiceSchemaElement,
        overallFields: List<GraphQLInputObjectField>,
        underlyingFields: List<GraphQLInputObjectField>,
    ): List<NadelSchemaValidationError> {
        val underlyingFieldsByName = underlyingFields.strictAssociateBy { it.name }

        val missingUnderlyingFieldErrors: List<NadelSchemaValidationError> = overallFields.mapNotNull { overallField ->
            val underlyingField = underlyingFieldsByName[overallField.name]

            if (underlyingField == null) {
                MissingUnderlyingInputField(parent, overallField)
            } else {
                // TODO: type check here
                getInputFieldErrors(parent, overallField, underlyingField)
                null
            }
        }

        return missingUnderlyingFieldErrors
    }

    private fun getInputFieldErrors(parent: NadelServiceSchemaElement,
                                    overallField: GraphQLInputObjectField,
                                    underlyingField: GraphQLInputObjectField,
    ): List<NadelSchemaValidationError> {
        TODO("Not yet implemented")
    }
}
