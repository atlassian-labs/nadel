package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.validation.NadelSchemaValidationError.Companion.missingUnderlyingEnum
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLEnumValueDefinition
import graphql.schema.GraphQLSchema

class NadelEnumValidation(
    overallSchema: GraphQLSchema,
    services: Map<String, Service>,
    private val service: Service,
) {
    fun getIssues(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationError> {
        return if (schemaElement.overall is GraphQLEnumType && schemaElement.underlying is GraphQLEnumType) {
            getIssues(
                parent = schemaElement,
                overallValues = schemaElement.overall.values,
                underlyingValues = schemaElement.underlying.values,
            )
        } else {
            emptyList()
        }
    }

    private fun getIssues(
        parent: NadelServiceSchemaElement,
        overallValues: List<GraphQLEnumValueDefinition>,
        underlyingValues: List<GraphQLEnumValueDefinition>,
    ): List<NadelSchemaValidationError> {
        val underlyingValuesByName = underlyingValues.strictAssociateBy { it.name }

        return overallValues.mapNotNull { overallValue ->
            val underlyingValue = underlyingValuesByName[overallValue.name]

            if (underlyingValue == null) {
                missingUnderlyingEnum(service, parent, overallValue)
            } else {
                null
            }
        }
    }
}
