package graphql.nadel.definition.coordinates

import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType

sealed interface NadelTypeCoordinates : NadelTopLevelDefinitionCoordinates {
    abstract override fun resolve(schema: GraphQLSchema): GraphQLType?
}
