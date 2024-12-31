package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.execution.ExecutionId
import graphql.nadel.Nadel
import graphql.nadel.ServiceExecution
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import kotlin.test.assertTrue

@UseHook
class `execution-id-is-transferred-from-provider-if-missing-in-input` : EngineTestHook {
    private var calls = 0

    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .executionIdProvider { _, _, _ ->
                ExecutionId.from("from-provider")
            }
    }

    override fun wrapServiceExecution(
        serviceName: String,
        baseTestServiceExecution: ServiceExecution,
    ): ServiceExecution {
        return ServiceExecution { params ->
            calls++
            assertTrue(params.executionId == ExecutionId.from("from-provider"))
            baseTestServiceExecution.execute(params)
        }
    }

    override fun assertResult(result: ExecutionResult) {
        assertTrue(calls > 0)
    }
}
