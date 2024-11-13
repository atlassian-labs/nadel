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
    ): NadelSchemaValidationResult {
        if (!isNamespacedOperationType(typeName = schemaElement.overall.name)) {
            return ok()
        }

        if (schemaElement.overall !is GraphQLObjectType) {
            return NamespacedTypeMustBeObject(schemaElement)
        }

        return ok()
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
