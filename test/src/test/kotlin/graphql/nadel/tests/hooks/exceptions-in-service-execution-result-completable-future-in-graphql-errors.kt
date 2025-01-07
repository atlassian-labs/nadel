package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.execution.ExecutionId
import graphql.nadel.NadelExecutionInput
import graphql.nadel.ServiceExecution
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.assertJsonKeys
import graphql.nadel.tests.util.data
import graphql.nadel.tests.util.errors
import graphql.nadel.tests.util.message
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.get
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.single
import java.util.concurrent.CompletableFuture

@UseHook
class `exceptions-in-service-execution-result-completable-future-in-graphql-errors` : EngineTestHook {
    override fun makeExecutionInput(builder: NadelExecutionInput.Builder): NadelExecutionInput.Builder {
        return super.makeExecutionInput(builder)
            .executionId(ExecutionId.from("test"))
    }

    override fun wrapServiceExecution(serviceName: String, baseTestServiceExecution: ServiceExecution): ServiceExecution {
        return ServiceExecution {
            CompletableFuture.completedFuture(null)
                .thenCompose {
                    throw RuntimeException("Pop goes the weasel")
                }
        }
    }

    override fun assertResult(result: ExecutionResult) {
        expectThat(result).data
            .isNotNull()
            .assertJsonKeys()["hello"]
            .isNull()
        expectThat(result).errors
            .single()
            .message
            .contains("Pop goes the weasel")
    }
}
