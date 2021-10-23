package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingEnumValue
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLEnumValueDefinition
import graphql.schema.GraphQLSchema

internal class NadelEnumValidation(
    overallSchema: GraphQLSchema,
    services: Map<String, Service>,
    private val service: Service,
) {
    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationError> {
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
    ): List<NadelSchemaValidationError> {
        val underlyingValuesByName = underlyingValues.strictAssociateBy { it.name }

        return overallValues.mapNotNull { overallValue ->
            val underlyingValue = underlyingValuesByName[overallValue.name]

            if (underlyingValue == null) {
                MissingUnderlyingEnumValue(service, parent, overallValue)
            } else {
                null
            }
        }
    }
}
