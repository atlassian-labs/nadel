package graphql.nadel.hooks

import graphql.GraphQLError
import graphql.nadel.Service

/**
 * Represents either a [Service] or an error that was generated when trying to resolve the service.
 */
// todo switch this to an Either<A, B> or sealed class thing, or union types one day…
data class ServiceOrError(
    val service: Service?,
    val error: GraphQLError?,
)
