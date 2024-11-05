package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.MissingConcreteTypes

internal class NadelInterfaceValidation {
    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement.Interface,
    ): NadelSchemaValidationResult {
        return validateHasImplementations(schemaElement)
    }

    context(NadelValidationContext)
    private fun validateHasImplementations(
        schemaElement: NadelServiceSchemaElement.Interface,
    ): NadelSchemaValidationResult {
        val underlyingSchema = schemaElement.service.underlyingSchema
        val underlyingInterfaceType = schemaElement.underlying
        val underlyingImplementations = underlyingSchema.getImplementations(underlyingInterfaceType)

        return if (underlyingImplementations.isEmpty()) {
            MissingConcreteTypes(schemaElement)
        } else {
            ok()
        }
    }
}
