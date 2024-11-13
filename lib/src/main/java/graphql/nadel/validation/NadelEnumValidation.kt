package graphql.nadel.validation

import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingEnumValue
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLEnumValueDefinition

internal class NadelEnumValidation {
    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): NadelSchemaValidationResult {
        return if (schemaElement.overall is GraphQLEnumType && schemaElement.underlying is GraphQLEnumType) {
            validate(
                parent = schemaElement,
                overallValues = schemaElement.overall.values,
                underlyingValues = schemaElement.underlying.values,
            )
        } else {
            ok()
        }
    }

    context(NadelValidationContext)
    private fun validate(
        parent: NadelServiceSchemaElement,
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
