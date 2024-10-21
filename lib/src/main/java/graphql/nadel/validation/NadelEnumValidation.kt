package graphql.nadel.validation

import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingEnumValue
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLEnumValueDefinition

internal class NadelEnumValidation {
    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationResult> {
        return if (schemaElement.overall is GraphQLEnumType && schemaElement.underlying is GraphQLEnumType) {
            validate(
                parent = schemaElement,
                overallValues = schemaElement.overall.values,
                underlyingValues = schemaElement.underlying.values,
            )
        } else {
            emptyList()
        }
    }

    private fun validate(
        parent: NadelServiceSchemaElement,
        overallValues: List<GraphQLEnumValueDefinition>,
        underlyingValues: List<GraphQLEnumValueDefinition>,
    ): List<NadelSchemaValidationResult> {
        val underlyingValuesByName = underlyingValues.strictAssociateBy { it.name }

        return overallValues.mapNotNull { overallValue ->
            val underlyingValue = underlyingValuesByName[overallValue.name]

            if (underlyingValue == null) {
                MissingUnderlyingEnumValue(parent, overallValue)
            } else {
                null
            }
        }
    }
}
