package graphql.nadel.hints

import graphql.nadel.Service

fun interface LegacyOperationNamesHint {
    /**
     * Determines whether to use the old `nadel_2_{service}_{operationName}` operation names
     * for the given service.
     *
     * @param service the service we want to determine whether to send legacy operation names to.
     * @return true to use the old format
     */
    operator fun invoke(service: Service): Boolean
}
