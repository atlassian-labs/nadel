package graphql.nadel.enginekt

import graphql.ExecutionInput

data class NadelExecutionContext(
    val executionInput: ExecutionInput,
    var serviceContexts: Map<String, Any>,
) {
    val userContext: Any?
        get() {
            return executionInput.context
        }

    fun getContextForService(serviceName: String): Any? {
        return serviceContexts[serviceName]
    }
}
