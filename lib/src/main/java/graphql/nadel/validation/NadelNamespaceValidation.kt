package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.NamespacedTypeMustBeObject

class NadelNamespaceValidation {
    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement.Type,
    ): NadelSchemaValidationResult {
        if (schemaElement.overall.name !in namespaceTypeNames) {
            return ok()
        }

        if (schemaElement !is NadelServiceSchemaElement.Object) {
            return NamespacedTypeMustBeObject(schemaElement)
        }

        return ok()
    }
}
