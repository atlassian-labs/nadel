package graphql.nadel.enginekt.defer

import graphql.ExecutionResult
import graphql.ExecutionResultImpl
import graphql.GraphQLError
import graphql.execution.ResultPath
import graphql.nadel.defer.DeferredExecutionResult

//TODO: can this be a list of something more well-defined? ResultPath maybe
class DeferredExecutionResultImpl(
    executionResultImpl: ExecutionResultImpl,
    private val path: List<Any>,
    private val label: String
) : ExecutionResultImpl(executionResultImpl), DeferredExecutionResult {

    override fun toSpecification(): Map<String, Any> {
        return super.toSpecification() + Pair("path", this.path)
    }

    override fun getPath(): List<Any> {
        return this.path
    }

    override fun getLabel(): String {
        return this.label
    }

    override fun toString() = "DeferredExecutionResultImpl{" +
            "errors=" + errors +
            ", path=" + path +
            ", label=" + label +
            ", data=" + getData() +
            ", dataPresent=" + isDataPresent +
            ", extensions=" + extensions +
            '}'

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

        fun build() = DeferredExecutionResultImpl(builder.build(), path!!, label!!)
    }
}