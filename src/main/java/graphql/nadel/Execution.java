package graphql.nadel;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.Internal;
import graphql.execution.ExecutionId;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.nextgen.ExecutionHelper;
import graphql.execution.nextgen.result.ResultNodesUtil;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.language.Document;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.nadel.engine.NadelExecutionStrategy;
import graphql.nadel.instrumentation.NadelInstrumentation;
import graphql.nadel.instrumentation.parameters.NadelInstrumentationExecuteOperationParameters;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Internal
class Execution {

    private final List<Service> services;
    private final GraphQLSchema overallSchema;
    private final NadelInstrumentation instrumentation;
    private final FieldInfos fieldInfos;
    private final ExecutionHelper executionHelper = new ExecutionHelper();
    private final NadelExecutionStrategy nadelExecutionStrategy;

    public Execution(List<Service> services, GraphQLSchema overallSchema, NadelInstrumentation instrumentation) {
        this.services = services;
        this.overallSchema = overallSchema;
        this.instrumentation = instrumentation;
        this.fieldInfos = createFieldsInfos();
        this.nadelExecutionStrategy = new NadelExecutionStrategy(services, this.fieldInfos, overallSchema);
    }

    public CompletableFuture<ExecutionResult> execute(ExecutionInput executionInput, Document document, ExecutionId executionId, InstrumentationState instrumentationState) {


        ExecutionHelper.ExecutionData executionData = executionHelper.createExecutionData(document, overallSchema, executionId, executionInput, instrumentationState);

        InstrumentationContext<ExecutionResult> instrumentationCtx = instrumentation.beginExecute(new NadelInstrumentationExecuteOperationParameters(executionData.executionContext, instrumentationState));

        CompletableFuture<RootExecutionResultNode> resultNodes = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection);

        CompletableFuture<ExecutionResult> result = resultNodes.thenApply(ResultNodesUtil::toExecutionResult);

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
