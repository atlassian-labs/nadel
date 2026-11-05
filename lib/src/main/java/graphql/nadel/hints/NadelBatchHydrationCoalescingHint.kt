package graphql.nadel.hints

import graphql.nadel.Service

/**
 * Enables coalescing type-level batch hydrations sent to a backing [service].
 *
 * Compatible consumers pool identifier arguments once. Equal selections share a deduplicated
 * selection lane; different selections may be packed as aliased roots in the same backing
 * operation. Hook partitions and service shards remain request boundaries, and the sum of root
 * cardinalities never exceeds the hydration's configured batch size.
 *
 * Only object-identifier matching is eligible. Indexed batch hydrations always retain their
 * ordinary isolated execution behavior, even when this hint returns `true`.
 *
 * Returning `true` for a service certifies that every otherwise compatible hydration sent to that
 * service is safe to pool, including hydrations governed by custom execution hooks. Hydration
 * instruction selection still happens for each consumer during preparation, and consumers for
 * which a custom hook selects another instruction remain isolated. For a shared group, however,
 * the batch-argument partition hook is invoked once over the pooled inputs with a representative
 * compatible instruction. Nadel verifies that the flattened partitions contain exactly the pooled
 * input multiset and keeps partitions as hard request boundaries, but it cannot verify that custom
 * partitioning logic establishes the correct semantic boundaries.
 *
 * The backing service execution and instrumentation handling must also accept one operation that
 * represents multiple hydration consumers, source fields, or top-level branches. Callers should
 * return `true` only after certifying both these shared-request semantics and the pooled custom-hook
 * semantics above for the service.
 */
fun interface NadelBatchHydrationCoalescingHint {
    operator fun invoke(service: Service): Boolean

    companion object {
        /**
         * The shared fail-closed default.
         *
         * Besides avoiding an allocation for every set of execution hints, this singleton lets
         * internal call sites cheaply distinguish the default from an explicitly supplied hint.
         */
        @JvmField
        val disabled: NadelBatchHydrationCoalescingHint = object : NadelBatchHydrationCoalescingHint {
            override fun invoke(service: Service): Boolean {
                return false
            }
        }
    }
}
