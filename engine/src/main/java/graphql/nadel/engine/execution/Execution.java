package graphql.nadel.engine.execution;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import graphql.Internal;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionId;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.nextgen.ExecutionHelper;
import graphql.execution.nextgen.FieldSubSelection;
import graphql.language.Document;
import graphql.nadel.NadelExecutionParams;
import graphql.nadel.Service;
import graphql.nadel.engine.BenchmarkContext;
import graphql.nadel.engine.FieldInfos;
import graphql.nadel.engine.NadelContext;
import graphql.nadel.engine.instrumentation.NadelEngineInstrumentation;
import graphql.nadel.engine.result.ResultComplexityAggregator;
import graphql.nadel.engine.result.ResultNodesUtil;
import graphql.nadel.engine.result.RootExecutionResultNode;
import graphql.nadel.hooks.ServiceExecutionHooks;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.nadel.instrumentation.parameters.NadelInstrumentRootExecutionResultParameters;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters;
import graphql.nadel.introspection.IntrospectionRunner;
import graphql.nadel.normalized.NormalizedQueryFactory;
import graphql.nadel.normalized.NormalizedQueryFromAst;
import graphql.schema.GraphQLSchema;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

@Internal
public class Execution {

    private final GraphQLSchema overallSchema;
    private final NadelInstrumentation instrumentation;
    private final IntrospectionRunner introspectionRunner;
    private final NadelExecutionStrategy nadelExecutionStrategy;

    private final ExecutionHelper executionHelper = new ExecutionHelper();
    private final NormalizedQueryFactory normalizedQueryFactory = new NormalizedQueryFactory();

    public Execution(List<Service> services,
                     GraphQLSchema overallSchema,
                     NadelInstrumentation instrumentation,
                     IntrospectionRunner introspectionRunner,
                     ServiceExecutionHooks serviceExecutionHooks,
                     Object userSuppliedContext) {
        this.overallSchema = overallSchema;
        this.instrumentation = instrumentation;
        this.introspectionRunner = introspectionRunner;
        FieldInfos fieldsInfos = FieldInfos.createFieldsInfos(overallSchema, services);
        if (userSuppliedContext instanceof BenchmarkContext) {
            BenchmarkContext.NadelExecutionStrategyArgs args = ((BenchmarkContext) userSuppliedContext).nadelExecutionStrategyArgs;
            args.services = services;
            args.fieldInfos = fieldsInfos;
            args.overallSchema = overallSchema;
            args.instrumentation = instrumentation;
            args.serviceExecutionHooks = serviceExecutionHooks;
        }

        this.nadelExecutionStrategy = new NadelExecutionStrategy(services, fieldsInfos, overallSchema, instrumentation, serviceExecutionHooks);
    }

    public CompletableFuture<ExecutionResult> execute(ExecutionInput executionInput,
                                                      Document document,
                                                      ExecutionId executionId,
                                                      InstrumentationState instrumentationState,
                                                      NadelExecutionParams nadelExecutionParams) {

        NormalizedQueryFromAst normalizedQueryFromAst = normalizedQueryFactory.createNormalizedQuery(overallSchema, document, executionInput.getOperationName(), executionInput.getVariables());

        NadelContext nadelContext = NadelContext.newContext()
                .userSuppliedContext(executionInput.getContext())
                .originalOperationName(document, executionInput.getOperationName())
                .artificialFieldsUUID(nadelExecutionParams.getArtificialFieldsUUID())
                .normalizedOverallQuery(normalizedQueryFromAst)
                .nadelExecutionHints(nadelExecutionParams.getNadelExecutionHints())
                .build();

        executionInput = executionInput.transform(builder -> builder.context(nadelContext));

        ExecutionHelper.ExecutionData executionData;
        try {
            executionData = executionHelper.createExecutionData(document, overallSchema, executionId, executionInput, instrumentationState);
        } catch (RuntimeException rte) {
            //
            // this is the same behavior as in graphql-java
            if (rte instanceof GraphQLError) {
                return completedFuture(new ExecutionResultImpl((GraphQLError) rte));
            }
            throw rte;
        }

        ExecutionContext executionContext = executionData.executionContext;
        FieldSubSelection fieldSubSelection = executionHelper.getFieldSubSelection(executionContext);

        NadelInstrumentationExecuteOperationParameters executeOperationParameters = buildInstrumentationParameters(instrumentationState, normalizedQueryFromAst, executionContext);
        CompletableFuture<InstrumentationContext<ExecutionResult>> instrumentationCtxFuture = instrumentation.beginExecute(executeOperationParameters);

        ExecutionInput finalExecutionInput = executionInput;

        return instrumentationCtxFuture.thenCompose(instrumentationCtx -> {

            CompletableFuture<ExecutionResult> result;
            ResultComplexityAggregator resultComplexityAggregator = new ResultComplexityAggregator();
            if (introspectionRunner.isIntrospectionQuery(executionContext, fieldSubSelection)) {
                result = introspectionRunner.runIntrospection(executionContext, fieldSubSelection, finalExecutionInput);
            } else {
                if (nadelContext.getUserSuppliedContext() instanceof BenchmarkContext) {
                    BenchmarkContext.NadelExecutionStrategyArgs args = ((BenchmarkContext) nadelContext.getUserSuppliedContext()).nadelExecutionStrategyArgs;
                    args.executionContext = executionContext;
                    args.fieldSubSelection = fieldSubSelection;
                    args.resultComplexityAggregator = resultComplexityAggregator;
                }
                CompletableFuture<RootExecutionResultNode> resultNodes = nadelExecutionStrategy.execute(executionContext, fieldSubSelection, resultComplexityAggregator);
                result = resultNodes.thenApply(rootResultNode -> {
                    if (nadelContext.getUserSuppliedContext() instanceof BenchmarkContext) {
                        ((BenchmarkContext) nadelContext.getUserSuppliedContext()).overallResult = rootResultNode;
                    }
                    if (instrumentation instanceof NadelEngineInstrumentation) {
                        rootResultNode = ((NadelEngineInstrumentation) instrumentation).instrumentRootExecutionResult(rootResultNode, new NadelInstrumentRootExecutionResultParameters(executionContext, normalizedQueryFromAst, instrumentationState));
                    }
                    return withNodeComplexity(ResultNodesUtil.toExecutionResult(rootResultNode), resultComplexityAggregator);
                });
            }

            // note this happens NOW - not when the result completes
            instrumentationCtx.onDispatched(result);
            result = result.whenComplete(instrumentationCtx::onCompleted);
            return result;
        });
    }

    private static NadelInstrumentationExecuteOperationParameters buildInstrumentationParameters(
            InstrumentationState instrumentationState, NormalizedQueryFromAst normalizedQueryFromAst, ExecutionContext executionContext
    ) {
        return new NadelInstrumentationExecuteOperationParameters(
                normalizedQueryFromAst,
                executionContext.getDocument(),
                executionContext.getGraphQLSchema(),
                executionContext.getVariables(),
                executionContext.getOperationDefinition(),
                instrumentationState,
                executionContext.getContext());
    }

    public ExecutionResult withNodeComplexity(ExecutionResult executionResult, ResultComplexityAggregator resultComplexityAggregator) {
        return ExecutionResultImpl.newExecutionResult().from(executionResult)
                .addExtension("resultComplexity", resultComplexityAggregator.snapshotResultComplexityData())
                .build();
    }
}
