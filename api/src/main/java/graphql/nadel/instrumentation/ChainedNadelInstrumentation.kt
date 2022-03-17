package graphql.nadel.instrumentation;

import graphql.ExecutionResult;
import graphql.execution.Async;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.language.Document;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationCreateStateParameters;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryExecutionParameters;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationQueryValidationParameters;
import graphql.validation.ValidationError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static graphql.Assert.assertNotNull;
import static java.util.stream.Collectors.toList;

/**
 * This allows you to chain together a number of {@link graphql.nadel.instrumentation.NadelInstrumentation} implementations
 * and run them in sequence.  The list order of instrumentation objects is always guaranteed to be followed and
 * the {@link graphql.execution.instrumentation.InstrumentationState} objects they create will be passed back to the originating
 * implementation.
 *
 * @see graphql.nadel.instrumentation.NadelInstrumentation
 */
public class ChainedNadelInstrumentation implements NadelInstrumentation {
    private final List<NadelInstrumentation> instrumentations;

    public ChainedNadelInstrumentation(List<NadelInstrumentation> instrumentations) {
        this.instrumentations = assertNotNull(instrumentations);
    }

    public List<NadelInstrumentation> getInstrumentations() {
        return new ArrayList<>(instrumentations);
    }

    protected InstrumentationState getStateFor(NadelInstrumentation instrumentation, InstrumentationState parametersInstrumentationState) {
        ChainedInstrumentationState chainedInstrumentationState = (ChainedInstrumentationState) parametersInstrumentationState;
        return chainedInstrumentationState.getState(instrumentation);
    }

    @Override
    public InstrumentationState createState(NadelInstrumentationCreateStateParameters parameters) {
        return new ChainedInstrumentationState(instrumentations, parameters);
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginQueryExecution(NadelInstrumentationQueryExecutionParameters parameters) {
        return new ChainedInstrumentationContext<>(instrumentations.stream()
            .map(instrumentation -> {
                InstrumentationState state = getStateFor(instrumentation, parameters.getInstrumentationState());
                return instrumentation.beginQueryExecution(parameters.withNewState(state));
            })
            .collect(toList()));
    }

    @Override
    public InstrumentationContext<Document> beginParse(NadelInstrumentationQueryExecutionParameters parameters) {
        return new ChainedInstrumentationContext<>(instrumentations.stream()
            .map(instrumentation -> {
                InstrumentationState state = getStateFor(instrumentation, parameters.getInstrumentationState());
                return instrumentation.beginParse(parameters.withNewState(state));
            })
            .collect(toList()));
    }

    @Override
    public InstrumentationContext<List<ValidationError>> beginValidation(NadelInstrumentationQueryValidationParameters parameters) {
        return new ChainedInstrumentationContext<>(instrumentations.stream()
            .map(instrumentation -> {
                InstrumentationState state = getStateFor(instrumentation, parameters.getInstrumentationState());
                return instrumentation.beginValidation(parameters.withNewState(state));
            })
            .collect(toList()));
    }

    @Override
    public CompletableFuture<InstrumentationContext<ExecutionResult>> beginExecute(NadelInstrumentationExecuteOperationParameters parameters) {
        CompletableFuture<List<InstrumentationContext<ExecutionResult>>> listCompletableFuture = Async.eachSequentially(instrumentations,
            (instrumentation, index, previousResults) -> {
                InstrumentationState state = getStateFor(instrumentation, parameters.getInstrumentationState());
                return instrumentation.beginExecute(parameters.withNewState(state));
            });

        return listCompletableFuture.thenApply(ChainedInstrumentationContext::new);
    }

    @Override
    public CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult executionResult, NadelInstrumentationQueryExecutionParameters parameters) {
        CompletableFuture<List<ExecutionResult>> resultsFuture = Async.eachSequentially(instrumentations, (instrumentation, index, prevResults) -> {
            InstrumentationState state = getStateFor(instrumentation, parameters.getInstrumentationState());
            ExecutionResult lastResult = prevResults.size() > 0 ? prevResults.get(prevResults.size() - 1) : executionResult;
            return instrumentation.instrumentExecutionResult(lastResult, parameters.withNewState(state));
        });
        return resultsFuture.thenApply((results) -> results.isEmpty() ? executionResult : results.get(results.size() - 1));
    }

    protected static class ChainedInstrumentationState implements InstrumentationState {
        private final Map<NadelInstrumentation, InstrumentationState> instrumentationStates;

        private ChainedInstrumentationState(List<NadelInstrumentation> instrumentations, NadelInstrumentationCreateStateParameters parameters) {
            instrumentationStates = new LinkedHashMap<>(instrumentations.size());
            instrumentations.forEach(i -> instrumentationStates.put(i, i.createState(parameters)));
        }

        private InstrumentationState getState(NadelInstrumentation instrumentation) {
            return instrumentationStates.get(instrumentation);
        }
    }

    protected static class ChainedInstrumentationContext<T> implements InstrumentationContext<T> {
        private final List<InstrumentationContext<T>> contexts;

        ChainedInstrumentationContext(List<InstrumentationContext<T>> contexts) {
            this.contexts = Collections.unmodifiableList(contexts);
        }

        @Override
        public void onDispatched(CompletableFuture<T> result) {
            contexts.forEach(context -> context.onDispatched(result));
        }

        @Override
        public void onCompleted(T result, Throwable t) {
            contexts.forEach(context -> context.onCompleted(result, t));
        }
    }
}
