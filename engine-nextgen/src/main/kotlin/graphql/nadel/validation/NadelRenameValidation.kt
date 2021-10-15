package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.enginekt.util.getFieldAt
import graphql.nadel.validation.NadelSchemaUtil.getRename
import graphql.nadel.validation.NadelSchemaValidationError.Companion.missingRename
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer

class NadelRenameValidation(
    private val service: Service,
    private val fieldValidation: NadelFieldValidation,
) {
    fun getIssues(
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
                    missingRename(service, parent, overallField, rename),
                )
            } else {
                fieldValidation.getIssues(parent, overallField, underlyingField)
            }
        }
    }
}
