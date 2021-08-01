package graphql.nadel.enginekt.defer

object FinalDeferredExecutionResult : DeferredExecutionResultImpl(
    label= null,
    path = null,
    hasNext = false,
    executionResultImpl = newExecutionResult().build()
) {
}