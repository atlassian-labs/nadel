package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.NamespacedTypeMustBeObject
import graphql.schema.GraphQLObjectType

class NadelNamespaceValidation {
    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationResult> {
        if (schemaElement.overall.name !in namespaceTypeNames) {
            return emptyList()
        }

        if (schemaElement.overall !is GraphQLObjectType) {
            return listOf(NamespacedTypeMustBeObject(schemaElement))
        }

        return emptyList()
    }
}
