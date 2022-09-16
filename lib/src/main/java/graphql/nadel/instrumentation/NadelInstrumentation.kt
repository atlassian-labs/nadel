package graphql.nadel.instrumentation

import graphql.ExecutionResult
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.SimpleInstrumentationContext.noOp
import graphql.language.Document
import graphql.nadel.instrumentation.parameters.NadelInstrumentationCreateStateParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationOnErrorParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryExecutionParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryValidationParameters
import graphql.nadel.instrumentation.parameters.NadelInstrumentationTimingParameters
import graphql.validation.ValidationError
import java.util.concurrent.CompletableFuture

/**
 * Provides the capability to instrument the execution steps of a Nadel GraphQL query.
 *
 *
 * Remember that graphql calls can cross threads so make sure you think about the thread safety of any instrumentation
 * code when you are writing it.
 *
 *
 * Each step gives back an [graphql.execution.instrumentation.InstrumentationContext] object.  This has two callbacks on it,
 * one for the step is `dispatched` and one for when the step has `completed`.  This is done because many of the "steps" are asynchronous
 * operations such as fetching data and resolving it into objects.
 */
interface NadelInstrumentation {
    /**
     * This will be called just before execution to create an object that is given back to all instrumentation methods
     * to allow them to have per execution request state
     *
     * @param parameters the parameters to this step
     * @return a state object that is passed to each method
     */
    fun createState(parameters: NadelInstrumentationCreateStateParameters): InstrumentationState? {
        return null
    }

    /**
     * This is called just before timing some specific Nadel code. See [NadelInstrumentationTimingParameters.step].
     *
     * After the timing of the step is complete [InstrumentationContext.onCompleted] is invoked.
     *
     * You can implement this function to record metrics in your Gateway service to determine what part of Nadel is taking up time.
     */
    fun onStepTimed(parameters: NadelInstrumentationTimingParameters) {
    }

    /**
     * This is called right at the start of query execution and its the first step in the instrumentation chain.
     *
     * @param parameters the parameters to this step
     * @return a non null [InstrumentationContext] object that will be called back when the step ends
     */
    fun beginQueryExecution(parameters: NadelInstrumentationQueryExecutionParameters): InstrumentationContext<ExecutionResult> {
        return noOp()
    }

    /**
     * This is called just before a query is parsed.
     *
     * @param parameters the parameters to this step
     * @return a non null [InstrumentationContext] object that will be called back when the step ends
     */
    fun beginParse(parameters: NadelInstrumentationQueryExecutionParameters): InstrumentationContext<Document> {
        return noOp()
    }

    /**
     * This is called just before the parsed query document is validated.
     *
     * @param parameters the parameters to this step
     * @return a non null [InstrumentationContext] object that will be called back when the step ends
     */
    fun beginValidation(parameters: NadelInstrumentationQueryValidationParameters): InstrumentationContext<List<ValidationError>> {
        return noOp()
    }

    /**
     * This is called just before the execution of the query operation is started.
     *
     * @param parameters the parameters to this step
     * @return a non null [InstrumentationContext] object that will be called back when the step ends
     */
    fun beginExecute(parameters: NadelInstrumentationExecuteOperationParameters): CompletableFuture<InstrumentationContext<ExecutionResult>> {
        return CompletableFuture.completedFuture(noOp())
    }

    /**
     * This is called to allow instrumentation to instrument the execution result in some way
     *
     * @param executionResult [java.util.concurrent.CompletableFuture] of the result to instrument
     * @param parameters      the parameters to this step
     * @return a new execution result completable future
     */
    fun instrumentExecutionResult(
        executionResult: ExecutionResult,
        parameters: NadelInstrumentationQueryExecutionParameters,
    ): CompletableFuture<ExecutionResult> {
        return CompletableFuture.completedFuture(executionResult)
    }

    fun onError(parameters: NadelInstrumentationOnErrorParameters<*>) {
    }
}
