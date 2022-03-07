package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.execution.AbortExecutionException
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.SimpleInstrumentationContext
import graphql.nadel.Nadel
import graphql.nadel.instrumentation.ChainedNadelInstrumentation
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryExecutionParameters
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.util.data
import graphql.nadel.tests.util.errors
import graphql.nadel.tests.util.message
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.single
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

@UseHook
class `execution-is-aborted-when-begin-execute-completes-exceptionally-using-chained-instrumentation` : EngineTestHook {
    var firstBeginExecuteCalled = 0
    var secondBeginExecuteCalled = 0
    var firstInstrumentExecutionResultCalled = 0
    var secondInstrumentExecutionResultCalled = 0

    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        val first = object : NadelInstrumentation {
            override fun beginExecute(
                parameters: NadelInstrumentationExecuteOperationParameters,
            ): CompletableFuture<InstrumentationContext<ExecutionResult>> {
                firstBeginExecuteCalled++
                return CompletableFuture.supplyAsync {
                    throw AbortExecutionException("instrumented-error")
                }
            }

            override fun instrumentExecutionResult(
                executionResult: ExecutionResult,
                parameters: NadelInstrumentationQueryExecutionParameters,
            ): CompletableFuture<ExecutionResult> {
                firstInstrumentExecutionResultCalled++
                return completedFuture(executionResult)
            }
        }

        val second = object : NadelInstrumentation {
            override fun beginExecute(
                parameters: NadelInstrumentationExecuteOperationParameters,
            ): CompletableFuture<InstrumentationContext<ExecutionResult>> {
                secondBeginExecuteCalled++
                return completedFuture(SimpleInstrumentationContext.noOp())
            }

            override fun instrumentExecutionResult(
                executionResult: ExecutionResult,
                parameters: NadelInstrumentationQueryExecutionParameters,
            ): CompletableFuture<ExecutionResult> {
                secondInstrumentExecutionResultCalled++
                return completedFuture(executionResult)
            }
        }

        val instrumentations = listOf(first, second)
        return builder.instrumentation(ChainedNadelInstrumentation(instrumentations))
    }

    override fun assertResult(result: ExecutionResult) {
        expectThat(firstBeginExecuteCalled).isEqualTo(1)
        expectThat(secondBeginExecuteCalled).isEqualTo(0)
        expectThat(firstInstrumentExecutionResultCalled).isEqualTo(1)
        expectThat(secondInstrumentExecutionResultCalled).isEqualTo(1)

        expectThat(result)
            .errors
            .single()
            .message
            .isEqualTo("instrumented-error")

        expectThat(result)
            .data
            .isNull()
    }
}
