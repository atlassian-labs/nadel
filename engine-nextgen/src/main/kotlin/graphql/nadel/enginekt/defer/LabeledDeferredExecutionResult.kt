package graphql.nadel.enginekt.defer

import graphql.ExecutionResult
import graphql.ExecutionResultImpl
import graphql.GraphQLError
import graphql.execution.ResultPath

class LabeledDeferredExecutionResult(
    executionResult: ExecutionResultImpl,
    //TODO: can this be a list of something more well-defined? ResultPath maybe
    path: List<Any>,
    label: String,
) : DeferredExecutionResultImpl(
    executionResultImpl = executionResult,
    label = label,
    path = path,
    hasNext = true,
) {

    companion object {
        fun newDeferredExecutionResult() = Builder()
    }

    class Builder {
        private var path: List<Any>? = null
        private var label: String? = null
        private val builder = newExecutionResult()

        fun path(path: ResultPath) = apply { this.path = path.toList() }
        fun label(label: String) = apply { this.label = label }
        fun from(executionResult: ExecutionResult) = apply { builder.from(executionResult) }
        fun addErrors(errors: List<GraphQLError>) = apply { builder.addErrors(errors) }

        fun build() = LabeledDeferredExecutionResult(builder.build(), path!!, label!!)
    }
}