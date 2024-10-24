package graphql.nadel.engine.blueprint

import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer

data class NadelVirtualTypeContext(
    /**
     * The container of the virtual field that created this mapping.
     */
    val virtualFieldContainer: GraphQLFieldsContainer,
    /**
     * The virtual field that created this mapping.
     */
    val virtualField: GraphQLFieldDefinition,
    val virtualTypeToBackingType: Map<String, String>,
    val backingTypeToVirtualType: Map<String, String>,
)
