package graphql.nadel.hints

import graphql.nadel.Service

/**
 * Enables coalescing type-level batch hydrations sent to a backing [service].
 *
 * A coalesced backing operation may contain several aliased hydration roots, but the sum of
 * their batch argument cardinalities does not exceed the hydration's configured batch size.
 * Larger groups are split into multiple backing operations.
 *
 * Only object-identifier matching is eligible. Indexed batch hydrations always retain their
 * ordinary isolated execution behavior, even when this hint returns `true`.
 *
 * This is intentionally service-aware so callers can opt in only after the service execution
 * and instrumentation handling for that backing service are compatible with shared requests.
 */
fun interface NadelBatchHydrationCoalescingHint {
    operator fun invoke(service: Service): Boolean
}
