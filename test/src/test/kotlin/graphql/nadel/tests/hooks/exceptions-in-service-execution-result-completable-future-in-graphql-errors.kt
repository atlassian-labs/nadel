package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.nadel.Nadel
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.assertJsonKeys
import graphql.nadel.tests.util.data
import graphql.nadel.tests.util.errors
import graphql.nadel.tests.util.message
import graphql.nadel.tests.util.serviceExecutionFactory
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.get
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.single
import java.util.concurrent.CompletableFuture

@UseHook
class `exceptions-in-service-execution-result-completable-future-in-graphql-errors` : EngineTestHook {
    private class PopGoesTheWeaselException() : Exception()

    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .serviceExecutionFactory {
                ServiceExecution {
                    CompletableFuture.completedFuture(null)
                        .thenCompose {
                            throw PopGoesTheWeaselException()
                        }
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
            .contains("An PopGoesTheWeaselException occurred invoking the service MyService")
    }
}
