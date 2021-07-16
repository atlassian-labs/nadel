package graphql.nadel.enginekt

import graphql.ExecutionInput
import graphql.PublicApi
import graphql.nadel.Service
import graphql.nadel.hooks.CreateServiceContextParams
import graphql.nadel.hooks.ServiceExecutionHooks
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

@PublicApi
data class NadelExecutionContext(
    val executionInput: ExecutionInput,
    val hooks: ServiceExecutionHooks,
) {
    private val serviceContexts = ConcurrentHashMap<String, CompletableFuture<Any?>>()

    val userContext: Any?
        get() {
            return executionInput.context
        }

    /**
     * Get the service context for a given service
     */
    fun getContextForService(service: Service): CompletableFuture<Any?> {
        return serviceContexts.getOrPut(service.name) {
            hooks.createServiceContext(
                CreateServiceContextParams.newParameters().service(service).build()
            )
        }
    }
}
