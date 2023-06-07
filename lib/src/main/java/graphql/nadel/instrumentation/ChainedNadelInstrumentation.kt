package graphql.nadel.instrumentation

import graphql.ExecutionResult
import graphql.execution.Async
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.language.Document
import graphql.nadel.instrumentation.parameters.NadelInstrumentationCreateStateParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationOnErrorParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryExecutionParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryValidationParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters
import graphql.validation.ValidationError
import java.util.Collections
import java.util.concurrent.CompletableFuture

/**
 * This allows you to chain together a number of [graphql.nadel.instrumentation.NadelInstrumentation] implementations
 * and run them in sequence.  The list order of instrumentation objects is always guaranteed to be followed and
 * the [graphql.execution.instrumentation.InstrumentationState] objects they create will be passed back to the originating
 * implementation.
 *
 * @see graphql.nadel.instrumentation.NadelInstrumentation
 */
class ChainedNadelInstrumentation(
    private val instrumentations: List<NadelInstrumentation>,
) : NadelInstrumentation {
    constructor(vararg instrumentations: NadelInstrumentation) : this(instrumentations.toList())

    fun getInstrumentations(): List<NadelInstrumentation> {
        return Collections.unmodifiableList(instrumentations)
    }

    internal fun getStateFor(
        instrumentation: NadelInstrumentation,
        parametersInstrumentationState: ChainedInstrumentationState,
    ): InstrumentationState? {
        return parametersInstrumentationState.getState(instrumentation)
    }

    override fun createState(parameters: NadelInstrumentationCreateStateParameters): InstrumentationState? {
        return ChainedInstrumentationState(instrumentations, parameters)
    }

    override fun onStepTimed(parameters: NadelInstrumentationTimingParameters) {
        instrumentations.forEach { instrumentation: NadelInstrumentation ->
            val state = getStateFor(instrumentation, parameters.getInstrumentationState()!!)
            instrumentation.onStepTimed(parameters.copy(instrumentationState = state))
        }
    }

    override fun onError(parameters: NadelInstrumentationOnErrorParameters) {
        instrumentations.forEach { instrumentation: NadelInstrumentation ->
            val state = getStateFor(instrumentation, parameters.getInstrumentationState()!!)
            instrumentation.onError(parameters.copy(instrumentationState = state))
        }
    }

    override fun beginQueryExecution(parameters: NadelInstrumentationQueryExecutionParameters): InstrumentationContext<ExecutionResult> {
        return ChainedInstrumentationContext(
            instrumentations
                .map { instrumentation: NadelInstrumentation ->
                    val state = getStateFor(instrumentation, parameters.getInstrumentationState()!!)
                    instrumentation.beginQueryExecution(parameters.copy(instrumentationState = state))
                }
        )
    }

    override fun beginParse(parameters: NadelInstrumentationQueryExecutionParameters): InstrumentationContext<Document> {
        return ChainedInstrumentationContext(
            instrumentations
                .map { instrumentation: NadelInstrumentation ->
                    val state = getStateFor(instrumentation, parameters.getInstrumentationState()!!)
                    instrumentation.beginParse(parameters.copy(instrumentationState = state))
                }
        )
    }

    override fun beginValidation(parameters: NadelInstrumentationQueryValidationParameters): InstrumentationContext<List<ValidationError>> {
        return ChainedInstrumentationContext(
            instrumentations
                .map { instrumentation: NadelInstrumentation ->
                    val state = getStateFor(instrumentation, parameters.getInstrumentationState()!!)
                    instrumentation.beginValidation(parameters.copy(instrumentationState = state))
                }
        )
    }

    override fun beginExecute(parameters: NadelInstrumentationExecuteOperationParameters): CompletableFuture<InstrumentationContext<ExecutionResult>> {
        val listCompletableFuture = Async
            .eachSequentially(instrumentations) { instrumentation: NadelInstrumentation, _: List<InstrumentationContext<ExecutionResult>> ->
                val state = getStateFor(instrumentation, parameters.getInstrumentationState()!!)
                instrumentation.beginExecute(parameters.copy(instrumentationState = state))
            }
        return listCompletableFuture
            .thenApply { contexts: List<InstrumentationContext<ExecutionResult>> ->
                ChainedInstrumentationContext(contexts)
            }
    }

    override fun instrumentExecutionResult(
        executionResult: ExecutionResult,
        parameters: NadelInstrumentationQueryExecutionParameters,
    ): CompletableFuture<ExecutionResult> {
        val resultsFuture = Async
            .eachSequentially(instrumentations) { instrumentation: NadelInstrumentation, prevResults: List<ExecutionResult> ->
                val state = getStateFor(instrumentation, parameters.getInstrumentationState()!!)
                val lastResult = prevResults.lastOrNull() ?: executionResult
                instrumentation.instrumentExecutionResult(
                    executionResult = lastResult,
                    parameters = parameters.copy(instrumentationState = state),
                )
            }

        return resultsFuture
            .thenApply { results: List<ExecutionResult> ->
                results.lastOrNull() ?: executionResult
            }
    }

    internal class ChainedInstrumentationState internal constructor(
        instrumentations: List<NadelInstrumentation>,
        parameters: NadelInstrumentationCreateStateParameters,
    ) : InstrumentationState {
        private val instrumentationStates = instrumentations
            .associateWith {
                it.createState(parameters)
            }

        internal fun getState(instrumentation: NadelInstrumentation): InstrumentationState? {
            return instrumentationStates[instrumentation]
        }
    }

    private class ChainedInstrumentationContext<T>(
        private val contexts: List<InstrumentationContext<T>>,
    ) : InstrumentationContext<T> {
        override fun onDispatched(result: CompletableFuture<T>?) {
            contexts.forEach { context: InstrumentationContext<T> ->
                context.onDispatched(result)
            }
        }

        override fun onCompleted(result: T?, t: Throwable?) {
            contexts.forEach { context: InstrumentationContext<T> ->
                context.onCompleted(result, t)
            }
        }
    }
}
