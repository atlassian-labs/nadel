package graphql.nadel.hints

import graphql.nadel.Service

fun interface ShortCircuitEmptyQueryHint {
    /**
     * Determines whether empty queries containing only top level __typename fields should be short-circuited without
     * calling the underlying service and executed on the internal introspection service
     *
     * @param service the service we are sending the query to
     * @return true to execute the query on the internal introspection service
     */
    operator fun invoke(service: Service): Boolean
}
