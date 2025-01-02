package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.execution.ExecutionId
import graphql.nadel.NadelExecutionInput
import graphql.nadel.ServiceExecution
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import kotlin.test.assertTrue

@UseHook
class `execution-id-is-transferred-from-input` : EngineTestHook {
    private var calls = 0

    override fun makeExecutionInput(
        builder: NadelExecutionInput.Builder,
    ): NadelExecutionInput.Builder {
        return builder
            .executionId(ExecutionId.from("from-input"))
    }

    override fun wrapServiceExecution(
        serviceName: String,
        baseTestServiceExecution: ServiceExecution,
    ): ServiceExecution {
        return ServiceExecution { params ->
            calls++
            assertTrue(params.executionId == ExecutionId.from("from-input"))
            baseTestServiceExecution.execute(params)
        }
    }

    override fun assertResult(result: ExecutionResult) {
        assertTrue(calls > 0)
    }
}

