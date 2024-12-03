package graphql.nadel.validation

import graphql.nadel.definition.renamed.NadelRenamedDefinition
import graphql.nadel.engine.blueprint.NadelDeepRenameFieldInstruction
import graphql.nadel.engine.blueprint.NadelFieldInstruction
import graphql.nadel.engine.blueprint.NadelRenameFieldInstruction
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.util.getFieldAt
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.validation.NadelSchemaValidationError.CannotRenameHydratedField
import graphql.nadel.validation.NadelSchemaValidationError.CannotRenamePartitionedField
import graphql.nadel.validation.NadelSchemaValidationError.MissingRename
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType

internal class NadelRenameValidation(
    private val fieldValidation: NadelFieldValidation,
) {
    context(NadelValidationContext)
    fun validate(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult {
        if (isHydrated(parent, overallField)) {
            return CannotRenameHydratedField(parent, overallField)
        }

        if (isPartitioned(parent, overallField)) {
            return CannotRenamePartitionedField(parent, overallField)
        }

        val rename = getRenamedOrNull(parent, overallField)
            ?: return ok()

        val underlyingField = parent.underlying.getFieldAt(rename.from)
            ?: return MissingRename(parent, overallField, rename)

        val result = fieldValidation.validate(parent, overallField, underlyingField)

        return if (parent.overall is GraphQLObjectType) {
            results(
                result,
                NadelValidatedFieldResult(
                    service = parent.service,
                    fieldInstruction = makeRenameFieldInstruction(parent, overallField, rename)
                ),
            )
        } else {
            result
        }
    }

    context(NadelValidationContext)
    private fun makeRenameFieldInstruction(
        parent: NadelServiceSchemaElement.FieldsContainer,
        overallField: GraphQLFieldDefinition,
        rename: NadelRenamedDefinition.Field,
    ): NadelFieldInstruction {
        val location = makeFieldCoordinates(
            parentType = parent.overall,
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
