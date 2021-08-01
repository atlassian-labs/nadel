package graphql.nadel.enginekt.defer

import graphql.ExecutionResultImpl
import graphql.nadel.defer.DeferredExecutionResult

class FirstDeferredExecutionResult(
    executionResultImpl: ExecutionResultImpl,
    //TODO: can this be a list of something more well-defined? ResultPath maybe
    private val hasNext: Boolean = true
) : DeferredExecutionResultImpl(executionResultImpl, null, null, true) {

}