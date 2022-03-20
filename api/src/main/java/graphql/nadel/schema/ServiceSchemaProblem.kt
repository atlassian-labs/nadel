package graphql.nadel.schema

import graphql.GraphQLError
import graphql.GraphQLException
import graphql.schema.idl.errors.SchemaProblem

/**
 * This exception wraps a [graphql.schema.idl.errors.SchemaProblem] and associates the specific
 * problems with a service name
 */
data class ServiceSchemaProblem(
    override val message: String,
    val serviceName: String,
    override val cause: SchemaProblem,
) : GraphQLException(message, cause) {
    val errors: List<GraphQLError>
        get() = cause.errors
}
