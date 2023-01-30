package graphql.nadel.hints

fun interface NewResultMergerHint {
    /**
     * Determines whether to use the new [graphql.nadel.NadelResultMerger] or the old
     * [graphql.nadel.engine.util.mergeResults].
     *
     * @return true to use the new result merger
     */
    operator fun invoke(): Boolean
}
