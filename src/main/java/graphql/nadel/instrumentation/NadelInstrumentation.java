package graphql.nadel.instrumentation;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.execution.instrumentation.DocumentAndVariables;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.language.Document;
import graphql.nadel.ServiceExecution;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationCreateStateParameters;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationFetchFieldParameters;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryExecutionParameters;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationServiceExecutionParameters;
import graphql.nadel.instrumentation.parameters.NadelNadelInstrumentationQueryValidationParameters;
import graphql.nadel.result.ExecutionResultNode;
import graphql.validation.ValidationError;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static graphql.execution.instrumentation.SimpleInstrumentationContext.noOp;

/**
 * Provides the capability to instrument the execution steps of a Nadel GraphQL query.
 *
 * Remember that graphql calls can cross threads so make sure you think about the thread safety of any instrumentation
 * code when you are writing it.
 *
 * Each step gives back an {@link graphql.execution.instrumentation.InstrumentationContext} object.  This has two callbacks on it,
 * one for the step is `dispatched` and one for when the step has `completed`.  This is done because many of the "steps" are asynchronous
 * operations such as fetching data and resolving it into objects.
 */
@SuppressWarnings("unused")
public interface NadelInstrumentation {

    /**
     * This will be called just before execution to create an object that is given back to all instrumentation methods
     * to allow them to have per execution request state
     *
     * @param parameters the parameters to this step
     *
     * @return a state object that is passed to each method
     */
    default InstrumentationState createState(NadelInstrumentationCreateStateParameters parameters) {
        return null;
    }

    /**
     * This is called right at the start of query execution and its the first step in the instrumentation chain.
     *
     * @param parameters the parameters to this step
     *
     * @return a non null {@link InstrumentationContext} object that will be called back when the step ends
     */
    default InstrumentationContext<ExecutionResult> beginQueryExecution(NadelInstrumentationQueryExecutionParameters parameters) {
        return noOp();
    }

    /**
     * This is called just before a query is parsed.
     *
     * @param parameters the parameters to this step
     *
     * @return a non null {@link InstrumentationContext} object that will be called back when the step ends
     */
    default InstrumentationContext<Document> beginParse(NadelInstrumentationQueryExecutionParameters parameters) {
        return noOp();
    }

    /**
     * This is called just before the parsed query document is validated.
     *
     * @param parameters the parameters to this step
     *
     * @return a non null {@link InstrumentationContext} object that will be called back when the step ends
     */
    default InstrumentationContext<List<ValidationError>> beginValidation(NadelNadelInstrumentationQueryValidationParameters parameters) {
        return noOp();
    }

    /**
     * This is called just before the execution of the query operation is started.
     *
     * @param parameters the parameters to this step
     *
     * @return a non null {@link InstrumentationContext} object that will be called back when the step ends
     */
    default InstrumentationContext<ExecutionResult> beginExecute(NadelInstrumentationExecuteOperationParameters parameters) {
        return noOp();
    }

    /**
     * This is called just before the execution a field fetch is started
     *
     * @param parameters the parameters to this step
     *
     * @return a non null {@link InstrumentationContext} object that will be called back when the step ends
     */
    default InstrumentationContext<ExecutionResultNode> beginFieldFetch(NadelInstrumentationFetchFieldParameters parameters) {
        return noOp();
    }


    /**
     * This is called to instrument a {@link graphql.ExecutionInput} before it is used to parse, validate
     * and execute a query, allowing you to adjust what query input parameters are used
     *
     * @param executionInput the execution input to be used
     * @param parameters     the parameters describing the field to be fetched
     *
     * @return a non null instrumented ExecutionInput, the default is to return to the same object
     */
    default ExecutionInput instrumentExecutionInput(ExecutionInput executionInput, NadelInstrumentationQueryExecutionParameters parameters) {
        return executionInput;
    }

    /**
     * This is called to instrument a {@link graphql.language.Document} and variables before it is used allowing you to adjust the query AST if you so desire
     *
     * @param documentAndVariables the document and variables to be used
     * @param parameters           the parameters describing the execution
     *
     * @return a non null instrumented DocumentAndVariables, the default is to return to the same objects
     */
    default DocumentAndVariables instrumentDocumentAndVariables(DocumentAndVariables documentAndVariables, NadelInstrumentationQueryExecutionParameters parameters) {
        return documentAndVariables;
    }

    /**
     * This is called to allow instrumentation to instrument the execution result in some way
     *
     * @param executionResult {@link java.util.concurrent.CompletableFuture} of the result to instrument
     * @param parameters      the parameters to this step
     *
     * @return a new execution result completable future
     */
    default CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult executionResult, NadelInstrumentationQueryExecutionParameters parameters) {
        return CompletableFuture.completedFuture(executionResult);
    }

    /**
     * This is called to allow instrumentation a service execution
     *
     * @param serviceExecution the {@link graphql.nadel.ServiceExecution} to be changed
     * @param parameters       the parameters to this step
     *
     * @return a new service execution
     */
    default ServiceExecution instrumentServiceExecution(ServiceExecution serviceExecution, NadelInstrumentationServiceExecutionParameters parameters) {
        return serviceExecution;
    }

}
