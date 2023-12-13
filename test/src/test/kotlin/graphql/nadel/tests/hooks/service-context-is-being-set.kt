package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.hooks.CreateServiceContextParams
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.util.serviceExecutionFactory
import java.util.concurrent.CompletableFuture

@UseHook
class `service-context-is-being-set` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        val serviceExecutionFactory = builder.serviceExecutionFactory

        return builder
            .executionHooks(object : NadelExecutionHooks {
                override fun createServiceContext(params: CreateServiceContextParams): CompletableFuture<Any?> {
                    return CompletableFuture.completedFuture("Context for ${params.service.name}")
                }
            })
            .serviceExecutionFactory(object : ServiceExecutionFactory by serviceExecutionFactory {
                override fun getServiceExecution(serviceName: String): ServiceExecution {
                    val default = serviceExecutionFactory.getServiceExecution(serviceName)

                    return ServiceExecution { params ->
                        assert(params.getServiceContext<String>() == "Context for MyService")

                        default.execute(params)
                    }
                }
            })
    }
}
