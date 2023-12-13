package graphql.nadel.hints

fun interface DeferSupportHint {
    /**
     * Adds support for the @defer directive on Nadel execution
     */
    operator fun invoke(): Boolean
}
