package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.MissingConcreteTypes
import graphql.schema.GraphQLInterfaceType

internal class NadelInterfaceValidation {
    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): NadelSchemaValidationResult {
        return if (schemaElement.overall is GraphQLInterfaceType) {
            validateHasImplementations(schemaElement)
        } else {
            ok()
        }
    }

    context(NadelValidationContext)
    private fun validateHasImplementations(
        schemaElement: NadelServiceSchemaElement,
    ): NadelSchemaValidationResult {
        val underlyingSchema = schemaElement.service.underlyingSchema
        val underlyingInterfaceType = schemaElement.underlying as GraphQLInterfaceType
        val underlyingImplementations = underlyingSchema.getImplementations(underlyingInterfaceType)

        return if (underlyingImplementations.isEmpty()) {
            MissingConcreteTypes(schemaElement)
        } else {
            ok()
        }
    }
}
