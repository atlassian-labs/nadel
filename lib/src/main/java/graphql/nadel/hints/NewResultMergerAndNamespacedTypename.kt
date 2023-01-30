package graphql.nadel.hints

fun interface NewResultMergerAndNamespacedTypename {
    /**
     * Determines whether to use the new [graphql.nadel.NadelResultMerger] or the old
     * [graphql.nadel.engine.util.mergeResults].
     *
     * AND
     *
     * Determines whether to use the internal [graphql.GraphQL] to resolve the
     * `__typename` field for namespaced fields.
     *
     * @return true to use the new result merger
     */
    operator fun invoke(): Boolean
}
