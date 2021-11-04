package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.MissingConcreteTypes
import graphql.schema.GraphQLInterfaceType

internal class NadelInterfaceValidation {
    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationError> {
        return if (schemaElement.overall is GraphQLInterfaceType) {
            validateHasImplementations(schemaElement)
        } else {
            emptyList()
        }
    }

    private fun validateHasImplementations(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationError> {
        val underlyingSchema = schemaElement.service.underlyingSchema
        val underlyingInterfaceType = schemaElement.underlying as GraphQLInterfaceType
        val underlyingImplementations = underlyingSchema.getImplementations(underlyingInterfaceType)

        return if (underlyingImplementations.isEmpty()) {
            listOf(
                MissingConcreteTypes(schemaElement)
            )
        } else {
            emptyList()
        }
    }
}
