package graphql.nadel.engine;

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
import graphql.execution.nextgen.result.ResultNodesUtil;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.language.Document;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.nadel.FieldInfo;
import graphql.nadel.FieldInfos;
import graphql.nadel.NadelExecutionParams;
import graphql.nadel.Service;
import graphql.nadel.hooks.ServiceExecutionHooks;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters;
import graphql.nadel.introspection.IntrospectionRunner;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

@Internal
public class Execution {

    private final List<Service> services;
    private final GraphQLSchema overallSchema;
    private final NadelInstrumentation instrumentation;
    private final IntrospectionRunner introspectionRunner;
    private final ExecutionHelper executionHelper = new ExecutionHelper();
    private final NadelExecutionStrategy nadelExecutionStrategy;

    public Execution(List<Service> services, GraphQLSchema overallSchema, NadelInstrumentation instrumentation, IntrospectionRunner introspectionRunner, ServiceExecutionHooks serviceExecutionHooks) {
        this.services = services;
        this.overallSchema = overallSchema;
        this.instrumentation = instrumentation;
        this.introspectionRunner = introspectionRunner;
        this.nadelExecutionStrategy = new NadelExecutionStrategy(services, createFieldsInfos(), overallSchema, instrumentation, serviceExecutionHooks);
    }

    public CompletableFuture<ExecutionResult> execute(ExecutionInput executionInput,
                                                      Document document,
                                                      ExecutionId executionId,
                                                      InstrumentationState instrumentationState,
                                                      NadelExecutionParams nadelExecutionParams) {

        NadelContext nadelContext = NadelContext.newContext()
                .userSuppliedContext(executionInput.getContext())
                .originalOperationName(document, executionInput.getOperationName())
                .artificialFieldsUUID(nadelExecutionParams.getArtificialFieldsUUID())
                .forkJoinPool(nadelExecutionParams.getForkJoinPool())
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
        FieldSubSelection fieldSubSelection = executionData.fieldSubSelection;

        InstrumentationContext<ExecutionResult> instrumentationCtx = instrumentation.beginExecute(new NadelInstrumentationExecuteOperationParameters(executionContext, instrumentationState));

        CompletableFuture<ExecutionResult> result;
        if (introspectionRunner.isIntrospectionQuery(executionContext, fieldSubSelection)) {
            result = introspectionRunner.runIntrospection(executionContext, fieldSubSelection, executionInput);
        } else {
            CompletableFuture<RootExecutionResultNode> resultNodes = nadelExecutionStrategy.execute(executionContext, fieldSubSelection);
            result = resultNodes.thenApply(ResultNodesUtil::toExecutionResult);
        }

        // note this happens NOW - not when the result completes
        instrumentationCtx.onDispatched(result);
        result = result.whenComplete(instrumentationCtx::onCompleted);
        return result;
    }

    private FieldInfos createFieldsInfos() {
        Map<GraphQLFieldDefinition, FieldInfo> fieldInfoByDefinition = new LinkedHashMap<>();

        for (Service service : services) {
            ObjectTypeDefinition queryType = service.getDefinitionRegistry().getQueryType();
            GraphQLObjectType schemaQueryType = overallSchema.getQueryType();
            for (FieldDefinition fieldDefinition : queryType.getFieldDefinitions()) {
                GraphQLFieldDefinition graphQLFieldDefinition = schemaQueryType.getFieldDefinition(fieldDefinition.getName());
                FieldInfo fieldInfo = new FieldInfo(FieldInfo.FieldKind.TOPLEVEL, service, graphQLFieldDefinition);
                fieldInfoByDefinition.put(graphQLFieldDefinition, fieldInfo);
            }
            ObjectTypeDefinition mutationType = service.getDefinitionRegistry().getMutationType();
            if (mutationType != null) {
                GraphQLObjectType schemaMutationType = overallSchema.getMutationType();
                for (FieldDefinition fieldDefinition : mutationType.getFieldDefinitions()) {
                    GraphQLFieldDefinition graphQLFieldDefinition = schemaMutationType.getFieldDefinition(fieldDefinition.getName());
                    FieldInfo fieldInfo = new FieldInfo(FieldInfo.FieldKind.TOPLEVEL, service, graphQLFieldDefinition);
                    fieldInfoByDefinition.put(graphQLFieldDefinition, fieldInfo);
                }
            }
        }
        return new FieldInfos(fieldInfoByDefinition);
    }
}
