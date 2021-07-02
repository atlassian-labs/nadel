package graphql.nadel.tests.hooks

import graphql.ExecutionResult
import graphql.ExecutionResultImpl
import graphql.execution.AbortExecutionException
import graphql.execution.instrumentation.InstrumentationContext
import graphql.nadel.Nadel
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryExecutionParameters
import graphql.nadel.instrumentation.parameters.NadelNadelInstrumentationQueryValidationParameters
import graphql.nadel.tests.EngineTestHook
import graphql.nadel.tests.KeepHook
import graphql.nadel.tests.NadelEngineType
import graphql.validation.ValidationError
import java.util.concurrent.CompletableFuture

@KeepHook
class `abort-begin-execute-within-instrumentation-will-still-call-enhancing-instrumentation-functions` :
    EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
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
                    return CompletableFuture.completedFuture(
                        ExecutionResultImpl
                            .newExecutionResult()
                            .from(executionResult)
                            .data("enhanced beginExecute")
                            .build(),
                    )
                }
            })
    }
}

@KeepHook
class `abort-begin-query-execution-within-instrumentation-will-still-call-enhancing-instrumentation-functions` :
    EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
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
                    return CompletableFuture.completedFuture(
                        ExecutionResultImpl
                            .newExecutionResult()
                            .from(executionResult)
                            .data("enhanced beginQueryExecution")
                            .build(),
                    )
                }
            })
    }
}

@KeepHook
class `abort-begin-validation-within-instrumentation-will-still-call-enhancing-instrumentation-functions` :
    EngineTestHook {
    override fun makeNadel(engineType: NadelEngineType, builder: Nadel.Builder): Nadel.Builder {
        return builder
            .instrumentation(object : NadelInstrumentation {
                override fun beginValidation(
                    parameters: NadelNadelInstrumentationQueryValidationParameters,
                ): InstrumentationContext<MutableList<ValidationError>> {
                    throw AbortExecutionException("beginValidation")
                }

                override fun instrumentExecutionResult(
                    executionResult: ExecutionResult,
                    parameters: NadelInstrumentationQueryExecutionParameters,
                ): CompletableFuture<ExecutionResult> {
                    return CompletableFuture.completedFuture(
                        ExecutionResultImpl
                            .newExecutionResult()
                            .from(executionResult)
                            .data("enhanced beginValidation")
                            .build(),
                    )
                }
            })
    }
}
