package graphql.nadel.validation

import graphql.nadel.engine.util.operationTypes
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.util.NamespacedUtil.isNamespacedField
import graphql.nadel.validation.NadelSchemaValidationError.NamespacedTypeMustBeObject
import graphql.schema.GraphQLObjectType

class NadelNamespaceValidation(
) {
    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationResult> {
        if (!isNamespacedOperationType(typeName = schemaElement.overall.name)) {
            return emptyList()
        }

        if (schemaElement.overall !is GraphQLObjectType) {
            return listOf(NamespacedTypeMustBeObject(schemaElement))
        }

        return emptyList()
    }

    context(NadelValidationContext)
    fun isNamespacedOperationType(typeName: String): Boolean {
        return engineSchema.operationTypes
            .any { operationType ->
                operationType.fields
                    .any { field ->
                        isNamespacedField(field) && field.type.unwrapAll().name == typeName
                    }
            }
    }
}
