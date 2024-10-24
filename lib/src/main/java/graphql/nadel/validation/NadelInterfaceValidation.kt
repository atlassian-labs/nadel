package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.MissingConcreteTypes
import graphql.schema.GraphQLInterfaceType

internal class NadelInterfaceValidation {
    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationResult> {
        return if (schemaElement.overall is GraphQLInterfaceType) {
            validateHasImplementations(schemaElement)
        } else {
            emptyList()
        }
    }

    context(NadelValidationContext)
    private fun validateHasImplementations(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationResult> {
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
