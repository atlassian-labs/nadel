package graphql.nadel;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.Internal;
import graphql.VisibleForTesting;
import graphql.execution.ExecutionId;
import graphql.execution.nextgen.ExecutionHelper;
import graphql.execution.nextgen.result.ObjectExecutionResultNode;
import graphql.execution.nextgen.result.ResultNodesUtil;
import graphql.language.Document;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.nadel.engine.NadelExecutionStrategy;
import graphql.parser.Parser;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Internal
public class Execution {

    private final List<Service> services;
    private final GraphQLSchema graphQLSchema;
    private final FieldInfos fieldInfos;

    @VisibleForTesting
    ExecutionHelper executionHelper = new ExecutionHelper();
    @VisibleForTesting
    Parser queryParser = new Parser();

    NadelExecutionStrategy nadelExecutionStrategy;

    public Execution(List<Service> services, GraphQLSchema graphQLSchema) {
        this.services = services;
        this.graphQLSchema = graphQLSchema;
        this.fieldInfos = createFieldsInfos();
        this.nadelExecutionStrategy = new NadelExecutionStrategy(services, this.fieldInfos);
    }

    public CompletableFuture<ExecutionResult> execute(NadelExecutionInput nadelExecutionInput) {
        Document document = parseQuery(nadelExecutionInput.getQuery());
        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .operationName(nadelExecutionInput.getOperationName())
                .variables(nadelExecutionInput.getVariables())
                .build();
        ExecutionHelper.ExecutionData executionData = executionHelper.createExecutionData(document, graphQLSchema, ExecutionId.generate(), executionInput);

        CompletableFuture<ObjectExecutionResultNode.RootExecutionResultNode> resultNodes = nadelExecutionStrategy.execute(executionData.executionContext, executionData.fieldSubSelection);
        return resultNodes.thenApply(ResultNodesUtil::toExecutionResult);
    }

    private Document parseQuery(String query) {
        return queryParser.parseDocument(query);
    }


    private FieldInfos createFieldsInfos() {
        Map<GraphQLFieldDefinition, FieldInfo> fieldInfoByDefinition = new LinkedHashMap<>();

        for (Service service : services) {
            ObjectTypeDefinition queryType = service.getDefinitionRegistry().getQueryType();
            for (FieldDefinition fieldDefinition : queryType.getFieldDefinitions()) {
                GraphQLFieldDefinition graphQLFieldDefinition = getGraphQLFieldDefinition(fieldDefinition);
                FieldInfo fieldInfo = new FieldInfo(FieldInfo.FieldKind.TOPLEVEL, service, graphQLFieldDefinition);
                fieldInfoByDefinition.put(graphQLFieldDefinition, fieldInfo);
            }
        }
        return new FieldInfos(fieldInfoByDefinition);
    }

    private GraphQLFieldDefinition getGraphQLFieldDefinition(FieldDefinition fieldDefinition) {
        return graphQLSchema.getQueryType().getFieldDefinition(fieldDefinition.getName());
    }

}
