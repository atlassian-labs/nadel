package graphql.nadel.validation

import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingEnumValue
import graphql.schema.GraphQLEnumValueDefinition

class NadelEnumValidation internal constructor() {
    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement.Enum,
    ): NadelSchemaValidationResult {
        return validate(
            parent = schemaElement,
            overallValues = schemaElement.overall.values,
            underlyingValues = schemaElement.underlying.values,
        )
    }

    context(NadelValidationContext)
    private fun validate(
        parent: NadelServiceSchemaElement.Enum,
        overallValues: List<GraphQLEnumValueDefinition>,
        underlyingValues: List<GraphQLEnumValueDefinition>,
    ): NadelSchemaValidationResult {
        val underlyingValuesByName = underlyingValues.strictAssociateBy { it.name }

        overallValues.forEach { overallValue ->
            underlyingValuesByName[overallValue.name]
                ?: return MissingUnderlyingEnumValue(parent, overallValue)
        }

        return ok()
    }
}
