package graphql.nadel.enginekt.schema

import graphql.nadel.OperationKind
import graphql.nadel.Service
import graphql.schema.GraphQLFieldDefinition

data class GraphQLFieldInfo(
    val service: Service,
    val operationKind: OperationKind,
    val field: GraphQLFieldDefinition,
)
