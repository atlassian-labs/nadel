package graphql.nadel.validation

import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldInputType
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingInputField
import graphql.nadel.validation.util.NadelInputValidationUtil.isMatchingInputTypeNameIgnoringNullableRenameScalar
import graphql.schema.GraphQLInputObjectField
import graphql.schema.GraphQLInputObjectType
import graphql.schema.GraphQLInputType

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

        return overallFields.flatMap { overallField ->
            validate(parent, overallField, underlyingFieldsByName)
        }
    }

    private fun validate(
            parent: NadelServiceSchemaElement,
            overallInputField: GraphQLInputObjectField,
            underlyingFieldsByName: Map<String, GraphQLInputObjectField>,
    ): List<NadelSchemaValidationError> {
        val underlyingInputField = underlyingFieldsByName[overallInputField.name]
        return if (underlyingInputField == null) {
            listOf(
                    MissingUnderlyingInputField(parent, overallInputField),
            )
        } else {
            if (!isMatchingInputTypeNameIgnoringNullableRenameScalar(overallInputField.type, underlyingInputField.type)) {
                listOf(
                        IncompatibleFieldInputType(parent, overallInputField, underlyingInputField)
                )
            } else {
                listOf()
            }
        }
    }

}
