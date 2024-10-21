package graphql.nadel.validation

import graphql.nadel.definition.hydration.isHydrated
import graphql.nadel.definition.renamed.getRenamedOrNull
import graphql.nadel.engine.util.getFieldAt
import graphql.nadel.validation.NadelSchemaValidationError.CannotRenameHydratedField
import graphql.nadel.validation.NadelSchemaValidationError.CannotRenamePartitionedField
import graphql.nadel.validation.NadelSchemaValidationError.MissingRename
import graphql.nadel.validation.util.NadelSchemaUtil.hasPartition
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer

internal class NadelRenameValidation(
    private val fieldValidation: NadelFieldValidation,
) {
    fun validate(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
    ): List<NadelSchemaValidationError> {
        if (overallField.isHydrated()) {
            return listOf(
                CannotRenameHydratedField(parent, overallField),
            )
        }

        if (hasPartition(overallField)) {
            return listOf(
                CannotRenamePartitionedField(parent, overallField),
            )
        }

        val rename = overallField.getRenamedOrNull()

        return if (rename == null) {
            listOf()
        } else {
            val underlyingFieldContainer = parent.underlying as GraphQLFieldsContainer
            val underlyingField = underlyingFieldContainer.getFieldAt(rename.from)
            if (underlyingField == null) {
                listOf(
                    MissingRename(parent, overallField, rename),
                )
            } else {
                fieldValidation.validate(parent, overallField, underlyingField)
            }
        }
    }
}
