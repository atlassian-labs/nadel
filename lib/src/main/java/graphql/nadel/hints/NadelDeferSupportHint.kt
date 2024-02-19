package graphql.nadel.hints

fun interface NadelDeferSupportHint {
    /**
     * Adds support for the @defer directive on Nadel execution
     */
    operator fun invoke(): Boolean
}
