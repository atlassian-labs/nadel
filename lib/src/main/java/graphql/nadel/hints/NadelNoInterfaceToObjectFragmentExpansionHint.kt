package graphql.nadel.hints

import graphql.nadel.Service

/**
 * When ON for a [service], an interface/union field the client selected at the abstract-type level is sent to
 * the underlying service **bare** rather than expanded into `... on ConcreteType` fragments, so a sibling type
 * the client didn't name can't be injected into the query. Per-service for gradual rollout; defaults to `false`.
 */
fun interface NadelNoInterfaceToObjectFragmentExpansionHint {
    operator fun invoke(service: Service): Boolean
}
