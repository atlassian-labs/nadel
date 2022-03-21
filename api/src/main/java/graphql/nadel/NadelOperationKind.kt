package graphql.nadel

import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import graphql.language.OperationDefinition.Operation as GraphQLJavaOperation

enum class NadelOperationKind(
    /**
     * See the [spec](https://spec.graphql.org/draft/#OperationType) definition.
     *
     * i.e. one of query, mutation, subscription
     *
     * LOWER CASE
     *
     * Used in the spec for [SchemaDefinition](https://spec.graphql.org/draft/#SchemaDefinition).
     *
     * ```
     * SchemaDefinition:
     *     {RootOperationTypeDefinition}
     * RootOperationTypeDefinition:
     *     OperationType: NamedType
     * ```
     */
    val operationType: String,
    /**
     * The default name of the operation type in the schema i.e. `Query` etc.
     *
     * Can be overridden by [graphql.language.SchemaDefinition] hence the _default_ part of the property name.
     */
    val defaultTypeName: String,
    /**
     * The equivalent GraphQL Java enum.
     * Type is an aliased import for [graphql.language.OperationDefinition.Operation].
     */
    val astOperation: GraphQLJavaOperation,
) {
    Query("query", "Query", GraphQLJavaOperation.QUERY),
    Mutation("mutation", "Mutation", GraphQLJavaOperation.MUTATION),
    Subscription("subscription", "Subscription", GraphQLJavaOperation.SUBSCRIPTION);

    /**
     * Nullable if [Mutation] or [Subscription] don't exist in the given [schema].
     */
    fun getType(schema: GraphQLSchema): GraphQLObjectType? {
        return when (this) {
            Query -> schema.queryType
            Mutation -> schema.mutationType
            Subscription -> schema.subscriptionType
        }
    }
}
