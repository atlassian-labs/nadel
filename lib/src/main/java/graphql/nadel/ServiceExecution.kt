package graphql.nadel

import java.util.concurrent.CompletableFuture

fun interface ServiceExecution {
    fun execute(serviceExecutionParameters: ServiceExecutionParameters): CompletableFuture<ServiceExecutionResult>
}
