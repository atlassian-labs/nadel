package graphql.nadel.hints

import graphql.nadel.Service

fun interface NadelBatchRootFieldsHint {
    /**
     * Global feature flag for root-field batching. When false, Nadel keeps its original behaviour
     * of making one service call per root field. Use this to feature flag the whole feature.
     */
    operator fun invoke(): Boolean

    /**
     * Per-service opt-in for root-field batching. Defaults to the global flag [invoke].
     *
     * Only affects sibling root fields with no shared namespace wrapper (e.g. the prefixed
     * `jira_foo`, `jira_bar` form). Namespaced fields are already batched per service regardless.
     *
     * @param service the service the root fields would be sent to
     * @return true to batch this service's root fields into a single call
     */
    operator fun invoke(service: Service): Boolean = invoke()
}
