package graphql.nadel.validation

import graphql.language.FieldDefinition
import graphql.nadel.Service
import graphql.nadel.dsl.ExtendedFieldDefinition
import graphql.nadel.dsl.FieldMappingDefinition
import graphql.nadel.dsl.UnderlyingServiceHydration
import graphql.nadel.enginekt.util.unwrapAll
import graphql.nadel.schema.NadelDirectives
import graphql.nadel.util.Util
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLType

object NadelSchemaUtil {
    fun getUnderlyingType(overallType: GraphQLNamedType, service: Service): GraphQLType? {
        return service.underlyingSchema.getType(getRenamedFrom(overallType) ?: overallType.name)
    }

    fun getHydration(field: GraphQLFieldDefinition): UnderlyingServiceHydration? {
        val def = field.definition
        if (def is ExtendedFieldDefinition) {
            return def.fieldTransformation?.underlyingServiceHydration
        }

        return NadelDirectives.createUnderlyingServiceHydration(field)
    }

    fun hasHydration(field: GraphQLFieldDefinition): Boolean {
        return hasHydration(field.definition)
    }

    fun hasHydration(def: FieldDefinition): Boolean {
        if (def is ExtendedFieldDefinition) {
            return def.fieldTransformation?.underlyingServiceHydration != null
        }

        return def.hasDirective(NadelDirectives.HYDRATED_DIRECTIVE_DEFINITION.name)
    }

    fun getRename(field: GraphQLFieldDefinition): FieldMappingDefinition? {
        val def = field.definition
        if (def is ExtendedFieldDefinition) {
            return def.fieldTransformation?.fieldMappingDefinition
        }

        return NadelDirectives.createFieldMapping(field)
    }

    fun hasRename(field: GraphQLFieldDefinition): Boolean {
        return hasRename(field.definition)
    }

    fun hasRename(def: FieldDefinition): Boolean {
        if (def is ExtendedFieldDefinition) {
            return def.fieldTransformation?.fieldMappingDefinition != null
        }

        return def.hasDirective(NadelDirectives.RENAMED_DIRECTIVE_DEFINITION.name)
    }

    fun getUnderlyingName(type: GraphQLNamedType): String {
        return getRenamedFrom(type) ?: type.name
    }

    fun getRenamedFrom(type: GraphQLNamedType): String? {
        return Util.getTypeMappingDefinitionFor(type.unwrapAll())?.underlyingName
    }
}
