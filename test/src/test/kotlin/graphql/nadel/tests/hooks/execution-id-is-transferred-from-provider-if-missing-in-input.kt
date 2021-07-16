package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.execution.ExecutionId
import graphql.nadel.Nadel
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.KeepHook
import graphql.nadel.tests.NadelEngineType
import graphql.nadel.tests.util.serviceExecutionFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan

@KeepHook
class `execution-id-is-transferred-from-provider-if-missing-in-input` : EngineTestHook {
    private var calls = 0

    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        val serviceExecutionFactory = builder.serviceExecutionFactory

        return builder
            .serviceExecutionFactory(object : ServiceExecutionFactory by serviceExecutionFactory {
                override fun getServiceExecution(serviceName: String): ServiceExecution {
                    val serviceExecution = serviceExecutionFactory.getServiceExecution(serviceName)
                    return ServiceExecution { params ->
                        calls++
                        expectThat(params.executionId).isEqualTo(ExecutionId.from("from-provider"))
                        serviceExecution.execute(params)
                    }
                }
            })
            .executionIdProvider { _, _, _ ->
                ExecutionId.from("from-provider")
            }
    }

    override fun assertResult(engineType: NadelEngineType, result: ExecutionResult) {
        expectThat(calls).isGreaterThan(0)
    }
}
