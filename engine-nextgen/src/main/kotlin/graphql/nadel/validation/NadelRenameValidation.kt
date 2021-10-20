package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.enginekt.util.getFieldAt
import graphql.nadel.validation.NadelSchemaUtil.getRename
import graphql.nadel.validation.NadelSchemaValidationError.MissingRename
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer

internal class NadelRenameValidation(
    private val service: Service,
    private val fieldValidation: NadelFieldValidation,
) {
    fun validate(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
    ): List<NadelSchemaValidationError> {
        val rename = getRename(overallField)

        return if (rename == null) {
            listOf()
        } else {
            val underlyingFieldContainer = parent.underlying as GraphQLFieldsContainer
            val underlyingField = underlyingFieldContainer.getFieldAt(rename.inputPath)
            if (underlyingField == null) {
                listOf(
                    MissingRename(service, parent, overallField, rename),
                )
            } else {
                fieldValidation.validate(parent, overallField, underlyingField)
            }
        }
    }
}
