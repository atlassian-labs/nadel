package graphql.nadel.enginekt.defer

import graphql.ExecutionResultImpl
import graphql.nadel.defer.DeferredExecutionResult

class DeferredExecutionResultImpl private constructor(
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

    companion object {
        fun newFirstExecutionResult(executionResultImpl: ExecutionResultImpl): DeferredExecutionResult {
            return DeferredExecutionResultImpl(
                executionResultImpl = executionResultImpl,
                hasNext = true,
                label = null,
                path = null
            )
        }

        fun newLabeledExecutionResult(
            executionResultImpl: ExecutionResultImpl,
            label: String,
            path: List<Any>,
            hasNext: Boolean = true
        ): DeferredExecutionResult {
            return DeferredExecutionResultImpl(
                executionResultImpl = executionResultImpl,
                hasNext = hasNext,
                label = label,
                path = path
            )
        }

        fun newFinalExecutionResult(): DeferredExecutionResult {
            return DeferredExecutionResultImpl(
                label = null,
                path = null,
                hasNext = false,
                executionResultImpl = newExecutionResult().build()
            )
        }
    }
}