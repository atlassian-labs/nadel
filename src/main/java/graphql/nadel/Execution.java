package graphql.nadel;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.Internal;
import graphql.VisibleForTesting;
import graphql.execution.ExecutionId;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.nextgen.ExecutionHelper;
import graphql.execution.nextgen.result.ResultNodesUtil;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.language.Document;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.nadel.engine.NadelExecutionStrategy;
import graphql.parser.Parser;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Internal
public class Execution {

    private final List<Service> services;
    private final GraphQLSchema overallSchema;
    private final FieldInfos fieldInfos;

    @VisibleForTesting
    ExecutionHelper executionHelper = new ExecutionHelper();
    @VisibleForTesting
    Parser queryParser = new Parser();

    NadelExecutionStrategy nadelExecutionStrategy;

    public Execution(List<Service> services, GraphQLSchema overallSchema) {
        this.services = services;
        this.overallSchema = overallSchema;
        this.fieldInfos = createFieldsInfos();
        this.nadelExecutionStrategy = new NadelExecutionStrategy(services, this.fieldInfos, overallSchema);
    }

    public CompletableFuture<ExecutionResult> execute(NadelExecutionInput nadelExecutionInput) {
        Document document = parseQuery(nadelExecutionInput.getQuery());
        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .operationName(nadelExecutionInput.getOperationName())
                .variables(nadelExecutionInput.getVariables())
                .build();

        // TODO get real instrumentation happening in Nadel - this for now
        InstrumentationState instrumentationState = new InstrumentationState() {
        };
        ExecutionHelper.ExecutionData executionData = executionHelper.createExecutionData(document, overallSchema, ExecutionId.generate(), executionInput, instrumentationState);

        CompletableFuture<RootExecutionResultNode> resultNodes = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection);
        return resultNodes.thenApply(ResultNodesUtil::toExecutionResult);
    }

    private Document parseQuery(String query) {
        return queryParser.parseDocument(query);
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
