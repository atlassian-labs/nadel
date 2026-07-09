package graphql.nadel.hints

import graphql.nadel.Service

fun interface NadelBatchRootFieldsHint {
    /**
     * Determines whether multiple top level (root) fields destined for the same service should be
     * combined into a single service call (i.e. one request), rather than one call per root field.
     *
     * This only affects sibling root fields with no shared namespace wrapper (e.g. the prefixed
     * `jira_foo`, `jira_bar` form). Namespaced fields are already batched per service regardless.
     *
     * @param service the service the root fields would be sent to
     * @return true to batch this service's root fields into a single call
     */
    operator fun invoke(service: Service): Boolean
}
