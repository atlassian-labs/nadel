package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.nadel.NadelExecutionInput
import graphql.nadel.ServiceExecution
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan

@UseHook
class `graphqlcontext-is-present-on-input` : EngineTestHook {
    private var calls = 0

    override fun makeExecutionInput(builder: NadelExecutionInput.Builder): NadelExecutionInput.Builder {
        return builder.graphqlContext("x", "y")
    }

    override fun wrapServiceExecution(
        serviceName: String,
        baseTestServiceExecution: ServiceExecution,
    ): ServiceExecution {
        return ServiceExecution { params ->
            calls++
            expectThat(params.graphQLContext.get("x") as String).isEqualTo("y")
            baseTestServiceExecution.execute(params)
        }
    }

    override fun assertResult(result: ExecutionResult) {
        expectThat(calls).isGreaterThan(0)
    }
}
