package graphql.nadel.hints

import graphql.nadel.Service

fun interface NadelBatchRootFieldsHint {
    /**
     * Global feature flag for batching non-namespaced root fields. Namespace batching is controlled
     * independently by [NadelBatchNamespacedRootFieldsHint].
     */
    operator fun invoke(): Boolean

    /**
     * Per-service opt-in for root-field batching. Defaults to the global flag [invoke].
     *
     * Only affects sibling root fields with no shared namespace wrapper (e.g. the prefixed
     * `jira_foo`, `jira_bar` form). Children within each namespace are already grouped per service.
     *
     * @param service the service the root fields would be sent to
     * @return true to batch this service's root fields into a single call
     */
    operator fun invoke(service: Service): Boolean = invoke()
}
