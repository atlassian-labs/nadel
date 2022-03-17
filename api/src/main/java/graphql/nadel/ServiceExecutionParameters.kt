package graphql.nadel

import graphql.cachecontrol.CacheControl
import graphql.execution.ExecutionId
import graphql.language.Document
import graphql.language.OperationDefinition
import graphql.normalized.ExecutableNormalizedField

// todo make constructor internal once we merge api/ and engine-nextgen/
class ServiceExecutionParameters constructor(
    val query: Document,
    val context: Any?,
    val variables: Map<String, Any>,
    val operationDefinition: OperationDefinition,
    val executionId: ExecutionId,
    val cacheControl: CacheControl?,
    private val serviceContext: Any?,

    /**
     * @return details abut this service hydration or null if it's not a hydration call
     */
    val hydrationDetails: ServiceExecutionHydrationDetails?,
    val executableNormalizedField: ExecutableNormalizedField,
) {
    fun <T> getServiceContext(): T? {
        @Suppress("UNCHECKED_CAST") // Trust caller
        return serviceContext as T?
    }
}

