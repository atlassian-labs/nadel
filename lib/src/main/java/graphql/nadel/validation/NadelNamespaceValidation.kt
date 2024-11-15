package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.NamespacedTypeMustBeObject
import graphql.schema.GraphQLObjectType

class NadelNamespaceValidation {
    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): NadelSchemaValidationResult {
        if (schemaElement.overall.name !in namespaceTypeNames) {
            return ok()
        }

        if (schemaElement.overall !is GraphQLObjectType) {
            return NamespacedTypeMustBeObject(schemaElement)
        }

        return ok()
    }
}
