package graphql.nadel;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.execution.ExecutionId;
import graphql.execution.ExecutionIdProvider;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.language.Document;
import graphql.nadel.engine.BenchmarkContext;
import graphql.nadel.engine.execution.Execution;
import graphql.nadel.hooks.ServiceExecutionHooks;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.nadel.introspection.IntrospectionRunner;
import graphql.schema.GraphQLSchema;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NadelEngine implements NadelExecutionEngine {
    /**
     * @return a builder of Nadel objects
     */
    public static Nadel.Builder newNadel() {
        return new Nadel.Builder().engineFactory(NadelEngine::new);
    }

    private final List<Service> services;
    private final GraphQLSchema overallSchema;
    private final NadelInstrumentation instrumentation;
    private final ServiceExecutionHooks serviceExecutionHooks;
    private final ExecutionIdProvider executionIdProvider;
    private final IntrospectionRunner introspectionRunner;

    public NadelEngine(Nadel nadel) {
        this.services = nadel.services;
        this.overallSchema = nadel.privateOverallSchema;
        this.instrumentation = nadel.instrumentation;
        this.serviceExecutionHooks = nadel.serviceExecutionHooks;
        this.executionIdProvider = nadel.executionIdProvider;
        this.introspectionRunner = nadel.introspectionRunner;
    }

    @NotNull
    @Override
    public CompletableFuture<ExecutionResult> execute(
            @NotNull ExecutionInput executionInput,
            @NotNull Document queryDocument,
            InstrumentationState instrumentationState,
            @NotNull NadelExecutionParams nadelExecutionParams
    ) {
        String query = executionInput.getQuery();
        String operationName = executionInput.getOperationName();
        Object context = executionInput.getContext();

        ExecutionId executionId = executionInput.getExecutionId();
        if (executionId == null) {
            executionId = executionIdProvider.provide(query, operationName, context);
        }

        if (executionInput.getContext() instanceof BenchmarkContext) {
            BenchmarkContext.ExecutionArgs executionArgs = ((BenchmarkContext) executionInput.getContext()).executionArgs;
            executionArgs.services = services;
            executionArgs.overallSchema = overallSchema;
            executionArgs.instrumentation = instrumentation;
            executionArgs.introspectionRunner = introspectionRunner;
            executionArgs.serviceExecutionHooks = serviceExecutionHooks;
            executionArgs.context = executionInput.getContext();
            executionArgs.executionInput = executionInput;
            executionArgs.document = queryDocument;
            executionArgs.executionId = executionId;
            executionArgs.instrumentationState = instrumentationState;
            executionArgs.nadelExecutionParams = nadelExecutionParams;
        }

        Execution execution = new Execution(services, overallSchema, instrumentation, introspectionRunner, serviceExecutionHooks, executionInput.getContext());

        return execution.execute(executionInput, queryDocument, executionId, instrumentationState, nadelExecutionParams);
    }
}
