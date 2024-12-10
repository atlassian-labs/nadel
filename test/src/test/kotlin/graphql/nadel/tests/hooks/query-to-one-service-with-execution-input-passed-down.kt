package graphql.nadel.tests.hooks

import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionInput
import graphql.nadel.NadelUserContext
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.util.serviceExecutionFactory
import kotlin.test.assertTrue

@UseHook
class `query-to-one-service-with-execution-input-passed-down` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        val serviceExecutionFactory = builder.serviceExecutionFactory

        return builder
            .serviceExecutionFactory(object : ServiceExecutionFactory by serviceExecutionFactory {
                override fun getServiceExecution(serviceName: String): ServiceExecution {
                    val default = serviceExecutionFactory.getServiceExecution(serviceName)

                    return ServiceExecution { params ->
                        assertTrue(params.context is TestContext)
                        default.execute(params)
                    }
                }
            })
    }

    override fun makeExecutionInput(
        builder: NadelExecutionInput.Builder,
    ): NadelExecutionInput.Builder {
        return builder
            .context(TestContext())
    }

    class TestContext : NadelUserContext
}
