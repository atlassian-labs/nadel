package graphql.nadel.hints

fun interface NadelCoroutineTimeoutSupportHint {
    /**
     * Adds support for timeout of coroutine execution
     */
    operator fun invoke(): Boolean
}