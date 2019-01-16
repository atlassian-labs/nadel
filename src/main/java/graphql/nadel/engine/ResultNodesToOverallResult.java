package graphql.nadel.engine;

import graphql.execution.ExecutionStepInfo;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.execution.nextgen.result.ListExecutionResultNode;
import graphql.execution.nextgen.result.ObjectExecutionResultNode;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;
import graphql.util.TreeTransformerUtil;

import static graphql.Assert.assertShouldNeverHappen;

public class ResultNodesToOverallResult {


    public RootExecutionResultNode convert(RootExecutionResultNode resultNode, GraphQLSchema overallSchema) {
        ResultNodesTransformer resultNodesTransformer = new ResultNodesTransformer();

        return (RootExecutionResultNode) resultNodesTransformer.transform(resultNode, new TraverserVisitorStub<ExecutionResultNode>() {
            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                ExecutionResultNode node = context.thisNode();
                ExecutionResultNode convertedNode;
                if (node instanceof RootExecutionResultNode) {
                    convertedNode = mapRootResultNode((RootExecutionResultNode) node);
                } else if (node instanceof ObjectExecutionResultNode) {
                    convertedNode = mapObjectResultNode((ObjectExecutionResultNode) node, overallSchema);
                } else if (node instanceof ListExecutionResultNode) {
                    convertedNode = mapListExecutionResultNode((ListExecutionResultNode) node, overallSchema);
                } else if (node instanceof LeafExecutionResultNode) {
                    convertedNode = mapLeafResultNode((LeafExecutionResultNode) node, overallSchema);
                } else {
                    return assertShouldNeverHappen();
                }
                return TreeTransformerUtil.changeNode(context, convertedNode);
            }
        });
    }

    private ListExecutionResultNode mapListExecutionResultNode(ListExecutionResultNode resultNode, GraphQLSchema overallSchema) {
        FetchedValueAnalysis fetchedValueAnalysis = mapFetchedValueAnalysis(resultNode.getFetchedValueAnalysis(), overallSchema);
        return new ListExecutionResultNode(fetchedValueAnalysis, resultNode.getChildren());
    }

    private ObjectExecutionResultNode mapObjectResultNode(ObjectExecutionResultNode objectResultNode, GraphQLSchema overallSchema) {
        FetchedValueAnalysis fetchedValueAnalysis = mapFetchedValueAnalysis(objectResultNode.getFetchedValueAnalysis(), overallSchema);
        return new ObjectExecutionResultNode(fetchedValueAnalysis, objectResultNode.getChildrenMap());
    }

    private RootExecutionResultNode mapRootResultNode(RootExecutionResultNode resultNode) {
        return resultNode;
    }

    private LeafExecutionResultNode mapLeafResultNode(LeafExecutionResultNode leafExecutionResultNode, GraphQLSchema overallSchema) {
        //TODO: handle hydration results somehow
        FetchedValueAnalysis fetchedValueAnalysis = mapFetchedValueAnalysis(leafExecutionResultNode.getFetchedValueAnalysis(), overallSchema);
        return new LeafExecutionResultNode(fetchedValueAnalysis, leafExecutionResultNode.getNonNullableFieldWasNullException());
    }

    private FetchedValueAnalysis mapFetchedValueAnalysis(FetchedValueAnalysis fetchedValueAnalysis, GraphQLSchema overallSchema) {
        ExecutionStepInfo mappedExecutionStepInfo = mapExecutionStepInfo(fetchedValueAnalysis.getExecutionStepInfo(), overallSchema);
        GraphQLObjectType mappedResolvedType = null;
        if (fetchedValueAnalysis.getValueType() == FetchedValueAnalysis.FetchedValueType.OBJECT && !fetchedValueAnalysis.isNullValue()) {
            mappedResolvedType = (GraphQLObjectType) overallSchema.getType(fetchedValueAnalysis.getResolvedType().getName());
        }
        //TODO: match underlying errors
        GraphQLObjectType finalMappedResolvedType = mappedResolvedType;
        return fetchedValueAnalysis.transfrom(builder -> {
            builder
                    .resolvedType(finalMappedResolvedType)
                    .executionStepInfo(mappedExecutionStepInfo);
        });
    }

    private ExecutionStepInfo mapExecutionStepInfo(ExecutionStepInfo executionStepInfo, GraphQLSchema overallSchema) {
        //TODO: handle __typename
        GraphQLOutputType fieldType = executionStepInfo.getType();
        GraphQLObjectType fieldContainer = executionStepInfo.getFieldContainer();
        GraphQLObjectType mappedFieldContainer = (GraphQLObjectType) overallSchema.getType(fieldContainer.getName());
        GraphQLOutputType mappedFieldType = (GraphQLOutputType) overallSchema.getType(fieldType.getName());
        GraphQLFieldDefinition fieldDefinition = executionStepInfo.getFieldDefinition();
        GraphQLFieldDefinition mappedFieldDefinition = mappedFieldContainer.getFieldDefinition(fieldDefinition.getName());
        return executionStepInfo.transform(builder -> builder
                .type(mappedFieldType)
                .fieldContainer(mappedFieldContainer)
                .fieldDefinition(mappedFieldDefinition));
    }


}
