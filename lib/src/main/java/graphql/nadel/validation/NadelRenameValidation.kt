package graphql.nadel.validation

import graphql.nadel.definition.hydration.isHydrated
import graphql.nadel.definition.renamed.NadelRenamedDefinition
import graphql.nadel.definition.renamed.getRenamedOrNull
import graphql.nadel.engine.blueprint.NadelDeepRenameFieldInstruction
import graphql.nadel.engine.blueprint.NadelFieldInstruction
import graphql.nadel.engine.blueprint.NadelRenameFieldInstruction
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.util.getFieldAt
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.validation.NadelSchemaValidationError.CannotRenameHydratedField
import graphql.nadel.validation.NadelSchemaValidationError.MissingRename
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

        val rename = overallField.getRenamedOrNull()
            ?: return emptyList()

        val underlyingFieldContainer = parent.underlying as GraphQLFieldsContainer
        val underlyingField = underlyingFieldContainer.getFieldAt(rename.from)
            ?: return listOf(
                MissingRename(parent, overallField, rename),
            )

        val result = fieldValidation.validate(parent, overallField, underlyingField)
        if (result.any { it is NadelSchemaValidationError }) {
            return result
        }

        return result + NadelFieldResult(
            service = parent.service,
            fieldInstruction = makeRenameFieldInstruction(parent, overallField, rename)
        )
    }

    private fun makeRenameFieldInstruction(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        rename: NadelRenamedDefinition.Field,
    ): NadelFieldInstruction {
        val location = makeFieldCoordinates(
            parentType = parent.overall as GraphQLFieldsContainer,
            field = overallField,
        )

        val underlyingName = rename.from.singleOrNull()
            ?: return NadelDeepRenameFieldInstruction(
                location = location,
                queryPathToField = NadelQueryPath(rename.from),
            )

        return NadelRenameFieldInstruction(
            location = location,
            underlyingName = underlyingName,
        )
    }
}
