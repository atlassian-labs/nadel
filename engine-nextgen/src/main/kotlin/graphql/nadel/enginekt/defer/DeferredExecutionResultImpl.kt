package graphql.nadel.enginekt.defer

import graphql.ExecutionResult
import graphql.ExecutionResultImpl
import graphql.GraphQLError
import graphql.execution.ResultPath
import graphql.nadel.defer.DeferredExecutionResult

abstract class DeferredExecutionResultImpl(
    executionResultImpl: ExecutionResultImpl,
    //TODO: can this be a list of something more well-defined? ResultPath maybe
    private val path: List<Any>?,
    private val label: String?,
    private val hasNext: Boolean
) : ExecutionResultImpl(executionResultImpl), DeferredExecutionResult {

    override fun toSpecification(): Map<String, Any?> {
        return super.toSpecification() +
                Pair("label", this.label) +
                Pair("path", this.path) +
                Pair("hasNext", this.hasNext())
    }

    override fun getPath(): List<Any>? = this.path

    override fun getLabel(): String? = this.label

    override fun hasNext(): Boolean = this.hasNext

    override fun toString() = "DeferredExecutionResultImpl{" +
            "errors=" + errors +
            ", path=" + path +
            ", label=" + label +
            ", data=" + getData() +
            ", dataPresent=" + isDataPresent +
            ", extensions=" + extensions +
            '}'

}