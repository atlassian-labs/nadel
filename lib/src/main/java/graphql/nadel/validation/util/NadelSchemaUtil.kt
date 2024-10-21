package graphql.nadel.validation.util

import graphql.language.FieldDefinition
import graphql.language.OperationDefinition
import graphql.nadel.Service
import graphql.nadel.dsl.FieldMappingDefinition
import graphql.nadel.dsl.NadelHydrationDefinition
import graphql.nadel.engine.util.operationTypes
import graphql.nadel.engine.blueprint.directives.NadelHydrationDefinition
import graphql.nadel.engine.blueprint.directives.getHydrationDefinitions
import graphql.nadel.engine.blueprint.directives.hasHydration
import graphql.nadel.schema.NadelDirectives
import graphql.nadel.util.NamespacedUtil
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLSchema

internal object NadelSchemaUtil {
    fun getUnderlyingType(overallType: GraphQLNamedType, service: Service): GraphQLNamedType? {
        return service.underlyingSchema.getType(getRenamedFrom(overallType) ?: overallType.name) as GraphQLNamedType?
    }

    fun hasHydration(field: GraphQLFieldDefinition): Boolean {
        return hasHydration(field.definition!!)
    }

    fun hasHydration(def: FieldDefinition): Boolean {
        return def.hasHydration()
    }

    fun getRename(field: GraphQLFieldDefinition): FieldMappingDefinition? {
        return NadelDirectives.createFieldMapping(field)
    }

    fun hasRename(field: GraphQLFieldDefinition): Boolean {
        return hasRename(field.definition!!)
    }

    fun hasRename(def: FieldDefinition): Boolean {
        return def.hasDirective(NadelDirectives.renamedDirectiveDefinition.name)
    }

    fun getUnderlyingName(type: GraphQLNamedType): String {
        return getRenamedFrom(type) ?: type.name
    }

    fun getRenamedFrom(type: GraphQLNamedType): String? {
        val asDirectivesContainer = type as? GraphQLDirectiveContainer ?: return null
        return NadelDirectives.createTypeMapping(asDirectivesContainer)?.underlyingName
    }

    fun hasPartition(field: GraphQLFieldDefinition): Boolean {
        return hasPartition(field.definition!!)
    }

    fun hasPartition(def: FieldDefinition): Boolean {
        return def.hasDirective(NadelDirectives.partitionDirectiveDefinition.name)
    }

    fun isOperation(type: GraphQLNamedType): Boolean {
        return type.name.equals(OperationDefinition.Operation.QUERY.toString(), ignoreCase = true)
            || type.name.equals(OperationDefinition.Operation.MUTATION.toString(), ignoreCase = true)
            || type.name.equals(OperationDefinition.Operation.SUBSCRIPTION.toString(), ignoreCase = true)
    }
}
