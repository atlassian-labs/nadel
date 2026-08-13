package graphql.nadel.hooks

/**
 * An opaque compatibility key for pooling batch hydrations that are processed by custom
 * [NadelExecutionHooks].
 *
 * Nadel compares keys using [equals] and [hashCode] but does not interpret [value]. Consumers
 * may only share a backing operation when their hooks return equal keys in addition to satisfying
 * Nadel's built-in compatibility checks.
 *
 * The value should therefore be immutable and have stable equality for the duration of an
 * execution.
 */
data class NadelBatchHydrationCoalescingKey(
    val value: Any,
) {
    companion object {
        internal val default = NadelBatchHydrationCoalescingKey(Default)

        private data object Default
    }
}
