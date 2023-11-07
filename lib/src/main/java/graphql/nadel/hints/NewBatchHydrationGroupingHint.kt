package graphql.nadel.hints

fun interface NewBatchHydrationGroupingHint {
    /**
     * Uses new grouping algorithm for batched hydrations.
     */
    operator fun invoke(): Boolean
}
