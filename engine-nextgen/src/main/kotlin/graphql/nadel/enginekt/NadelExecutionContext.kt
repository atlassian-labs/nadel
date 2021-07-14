package graphql.nadel.enginekt

import graphql.ExecutionInput
import graphql.nadel.Service
import graphql.nadel.hooks.CreateServiceContextParams
import graphql.nadel.hooks.ServiceExecutionHooks
import kotlinx.coroutines.future.await
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

data class NadelExecutionContext(
    val executionInput: ExecutionInput,
    val hooks: ServiceExecutionHooks,
) {
    private val serviceContexts = ConcurrentHashMap<String, Optional<Any>>()

    val userContext: Any?
        get() {
            return executionInput.context
        }

    suspend fun getContextForService(service: Service): Any? {
        return serviceContexts.getOrPut(service.name) {
            // Optional here because not all maps take null values
            Optional.ofNullable(
                hooks.createServiceContext(
                    CreateServiceContextParams.newParameters().service(service).build()
                ).await()
            )
        }.orElse(null)
    }
}
