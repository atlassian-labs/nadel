package graphql.nadel.hints

fun interface NadelJavaTransformFutureFastPathHint {
    /**
     * Enables reading completed Java transform futures without creating Deferred wrappers.
     */
    operator fun invoke(): Boolean
}
