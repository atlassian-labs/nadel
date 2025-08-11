package graphql.nadel.util

import graphql.language.ObjectTypeDefinition
import graphql.nadel.Service
import graphql.nadel.engine.util.isExtensionDef
import graphql.nadel.engine.util.isOperationType
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.schema.NadelDirectives.namespacedDirectiveDefinition
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import javax.swing.text.html.HTML.Tag.U

object NamespacedUtil {
    fun serviceOwnsNamespacedField(namespacedObjectTypeName: String, service: Service): Boolean {
        return service
            .definitionRegistry
            .getDefinitions(namespacedObjectTypeName)
            .asSequence()
            .filterIsInstance<ObjectTypeDefinition>()
            .filterNot { it.isExtensionDef }
            .any { it.name == namespacedObjectTypeName }
    }

    fun isNamespacedField(overallField: ExecutableNormalizedField, schema: GraphQLSchema): Boolean {
        return overallField
            .getFieldDefinitions(schema)
            .any {
                isNamespacedField(it)
            }
    }

    fun isNamespacedField(field: GraphQLFieldDefinition): Boolean {
        return field.hasAppliedDirective(namespacedDirectiveDefinition.name)
    }

    /**
     * So we have quite a few fields that do not have `@namespaced` that some parts of our
     * code just check if a field looks like it should have `@namespaced` on it.
     *
     * Instead of fixing it properly, we introduce a common helper util here to identify them.
     */
    fun isNamespacedFieldLike(field: GraphQLFieldDefinition): Boolean {
        return isNamespacedField(field)
            || (field.arguments.isEmpty() && field.type.unwrapNonNull() is GraphQLObjectType)
    }

    fun isNamespacedFieldLike(service: Service, rootLevelField: ExecutableNormalizedField): Boolean {
        val underlyingSchema = service.underlyingSchema
        val parentType = underlyingSchema.getTypeAs<GraphQLObjectType>(rootLevelField.singleObjectTypeName)

        if (!underlyingSchema.isOperationType(parentType)) {
            return false
        }

        val field = parentType.getField(rootLevelField.name)
            ?: return false

        return isNamespacedFieldLike(field)
    }
}
