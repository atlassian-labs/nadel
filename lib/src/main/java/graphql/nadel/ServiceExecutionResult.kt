package graphql.nadel

import graphql.ExecutionResult
import graphql.ExecutionResultImpl
import graphql.nadel.engine.util.extensions
import graphql.nadel.util.ErrorUtil

data class ServiceExecutionResult @JvmOverloads constructor(
    val data: MutableMap<String, Any?> = LinkedHashMap(),
    val errors: MutableList<MutableMap<String, Any?>> = ArrayList(),
    val extensions: MutableMap<String, Any?> = LinkedHashMap(),
) {
    fun toExecutionResult(): ExecutionResult {
        return ExecutionResultImpl.newExecutionResult()
            .data(data)
            .errors(ErrorUtil.createGraphQLErrorsFromRawErrors(errors))
            .extensions(extensions)
            .build()
    }
}
