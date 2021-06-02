package graphql.nadel.enginekt

import graphql.ExecutionInput

data class NadelExecutionContext(
    val executionInput: ExecutionInput,
) {
    val userContext: Any?
        get() {
            return executionInput.context
        }
}
