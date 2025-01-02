package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.ExecutionResultImpl
import graphql.execution.AbortExecutionException
import graphql.execution.instrumentation.InstrumentationContext
import graphql.nadel.Nadel
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryExecutionParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryValidationParameters
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.UseHook
import graphql.validation.ValidationError
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

@UseHook
class `abort-begin-execute-within-instrumentation-still-calls-enhancing-instrumentation` :
    EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .instrumentation(object : NadelInstrumentation {
                override fun beginExecute(
                    parameters: NadelInstrumentationExecuteOperationParameters,
                ): CompletableFuture<InstrumentationContext<ExecutionResult>> {
                    throw AbortExecutionException("beginExecute")
                }

                override fun instrumentExecutionResult(
                    executionResult: ExecutionResult,
                    parameters: NadelInstrumentationQueryExecutionParameters,
                ): CompletableFuture<ExecutionResult> {
                    return completedFuture(
                        ExecutionResultImpl
                            .newExecutionResult()
                            .from(executionResult)
                            .data(mapOf("step" to "beginExecute"))
                            .build(),
                    )
                }
            })
    }
}

@UseHook
class `abort-begin-execute-in-cf-within-instrumentation-still-calls-enhancing-instrumentation` : EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .instrumentation(object : NadelInstrumentation {
                override fun beginExecute(
                    parameters: NadelInstrumentationExecuteOperationParameters,
                ): CompletableFuture<InstrumentationContext<ExecutionResult>> {
                    return completedFuture(null)
                        .thenCompose {
                            throw AbortExecutionException("beginExecute")
                        }
                }

                override fun instrumentExecutionResult(
                    executionResult: ExecutionResult,
                    parameters: NadelInstrumentationQueryExecutionParameters,
                ): CompletableFuture<ExecutionResult> {
                    return completedFuture(
                        ExecutionResultImpl
                            .newExecutionResult()
                            .from(executionResult)
                            .data(mapOf("step" to "beginExecute"))
                            .build(),
                    )
                }
            })
    }
}

@UseHook
class `abort-begin-query-execution-within-instrumentation-still-calls-enhancing-instrumentation` :
    EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .instrumentation(object : NadelInstrumentation {
                override fun beginQueryExecution(
                    parameters: NadelInstrumentationQueryExecutionParameters,
                ): InstrumentationContext<ExecutionResult> {
                    throw AbortExecutionException("beginQueryExecution")
                }

                override fun instrumentExecutionResult(
                    executionResult: ExecutionResult,
                    parameters: NadelInstrumentationQueryExecutionParameters,
                ): CompletableFuture<ExecutionResult> {
                    return completedFuture(
                        ExecutionResultImpl
                            .newExecutionResult()
                            .from(executionResult)
                            .data(mapOf("step" to "beginQueryExecution"))
                            .build(),
                    )
                }
            })
    }
}

@UseHook
class `abort-begin-validation-within-instrumentation-still-calls-enhancing-instrumentation` :
    EngineTestHook {
    override fun makeNadel(builder: Nadel.Builder): Nadel.Builder {
        return builder
            .instrumentation(object : NadelInstrumentation {
                override fun beginValidation(
                    parameters: NadelInstrumentationQueryValidationParameters,
                ): InstrumentationContext<List<ValidationError>> {
                    throw AbortExecutionException("beginValidation")
                }

                override fun instrumentExecutionResult(
                    executionResult: ExecutionResult,
                    parameters: NadelInstrumentationQueryExecutionParameters,
                ): CompletableFuture<ExecutionResult> {
                    return completedFuture(
                        ExecutionResultImpl
                            .newExecutionResult()
                            .from(executionResult)
                            .data(mapOf("step" to "beginValidation"))
                            .build(),
                    )
                }
            })
    }
}
