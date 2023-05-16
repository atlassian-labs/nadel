package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionInput
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.util.serviceExecutionFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan

@UseHook
class `graphqlcontext-is-present-on-input` : EngineTestHook {
    private var calls = 0

    override fun makeExecutionInput(builder: NadelExecutionInput.Builder): NadelExecutionInput.Builder {
        return builder.graphqlContext("x", "y")
    }

    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        val serviceExecutionFactory = builder.serviceExecutionFactory

        return builder
            .serviceExecutionFactory(object : ServiceExecutionFactory by serviceExecutionFactory {
                override fun getServiceExecution(serviceName: String): ServiceExecution {
                    val serviceExecution = serviceExecutionFactory.getServiceExecution(serviceName)
                    return ServiceExecution { params ->
                        calls++
                        expectThat(params.graphQLContext.get("x") as String).isEqualTo("y")
                        serviceExecution.execute(params)
                    }
                }
            })
    }

    override fun assertResult(result: ExecutionResult) {
        expectThat(calls).isGreaterThan(0)
    }
}
