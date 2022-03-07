package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.ExecutionResultImpl
import graphql.execution.AbortExecutionException
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.nadel.Nadel
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationCreateStateParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryExecutionParameters
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.nadel.tests.util.data
import graphql.nadel.tests.util.errors
import graphql.nadel.tests.util.extensions
import graphql.nadel.tests.util.getToString
import graphql.nadel.tests.util.message
import strikt.api.expectThat
import strikt.assertions.get
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.single
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

@UseHook
class `execution-is-aborted-when-begin-execute-completes-exceptionally` : EngineTestHook {
    var instrumentationParams: NadelInstrumentationExecuteOperationParameters? = null
    var resultBeforeFinalInstrumentation: ExecutionResult? = null

    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .instrumentation(object : NadelInstrumentation {
                override fun createState(parameters: NadelInstrumentationCreateStateParameters): InstrumentationState {
                    return object : InstrumentationState {
                        override fun toString(): String {
                            return "so-annoying"
                        }
                    }
                }

                override fun beginExecute(
                    parameters: NadelInstrumentationExecuteOperationParameters,
                ): CompletableFuture<InstrumentationContext<ExecutionResult?>> {
                    instrumentationParams = parameters
                    throw AbortExecutionException("instrumented-error")
                }

                override fun instrumentExecutionResult(
                    executionResult: ExecutionResult,
                    parameters: NadelInstrumentationQueryExecutionParameters,
                ): CompletableFuture<ExecutionResult?> {
                    resultBeforeFinalInstrumentation = executionResult
                    return completedFuture(
                        ExecutionResultImpl.Builder()
                            .from(executionResult)
                            .addExtension("instrumentedExtension", "dummy extension")
                            .build(),
                    )
                }
            })
    }

    override fun assertResult(result: ExecutionResult) {
        expectThat(result)
            .extensions["instrumentedExtension"]
            .isEqualTo("dummy extension")

        expectThat(result)
            .data
            .isNull()

        expectThat(resultBeforeFinalInstrumentation)
            .isNotNull()
            .errors
            .single()
            .message
            .isEqualTo("instrumented-error")

        expectThat(instrumentationParams)
            .isNotNull()
            .get {
                getInstrumentationState<InstrumentationState>()
            }
            .getToString()
            .isEqualTo("so-annoying")
    }
}
