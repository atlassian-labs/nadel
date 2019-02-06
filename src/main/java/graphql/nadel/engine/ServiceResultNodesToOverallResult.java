package graphql.nadel.engine;

import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.execution.nextgen.result.ListExecutionResultNode;
import graphql.execution.nextgen.result.ObjectExecutionResultNode;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.language.Field;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.schema.GraphQLSchema;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;
import graphql.util.TreeTransformerUtil;

import java.util.LinkedHashMap;
import java.util.Map;

import static graphql.Assert.assertShouldNeverHappen;

public class ServiceResultNodesToOverallResult {

    FetchedAnalysisMapper fetchedAnalysisMapper = new FetchedAnalysisMapper();


    //TODO: the return type is not really ready to return hydration results, which can be used as input for new queries
    public RootExecutionResultNode convert(RootExecutionResultNode resultNode, GraphQLSchema overallSchema, Map<Field, FieldTransformation> transformationMap) {
        ResultNodesTransformer resultNodesTransformer = new ResultNodesTransformer();

        RootExecutionResultNode newRoot = (RootExecutionResultNode) resultNodesTransformer.transform(resultNode, new TraverserVisitorStub<ExecutionResultNode>() {
            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                ExecutionResultNode node = context.thisNode();
                ExecutionResultNode convertedNode;
                if (node instanceof RootExecutionResultNode) {
                    convertedNode = mapRootResultNode((RootExecutionResultNode) node, transformationMap);
                } else if (node instanceof ObjectExecutionResultNode) {
                    convertedNode = mapObjectResultNode((ObjectExecutionResultNode) node, overallSchema, transformationMap);
                } else if (node instanceof ListExecutionResultNode) {
                    convertedNode = mapListExecutionResultNode((ListExecutionResultNode) node, overallSchema, transformationMap);
                } else if (node instanceof LeafExecutionResultNode) {
                    convertedNode = mapLeafResultNode((LeafExecutionResultNode) node, overallSchema, transformationMap);
                } else {
                    return assertShouldNeverHappen();
                }
                return TreeTransformerUtil.changeNode(context, convertedNode);
            }

        });

        newRoot = (RootExecutionResultNode) resultNodesTransformer.transform(newRoot, new TraverserVisitorStub<ExecutionResultNode>() {
            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                ExecutionResultNode node = context.thisNode();
                if (node instanceof ObjectExecutionResultNode) {
                    Map<String, ExecutionResultNode> newChildren = new LinkedHashMap<>();
                    Map<String, ExecutionResultNode> children = ((ObjectExecutionResultNode) node).getChildrenMap();
                    for (String key : children.keySet()) {
                        ExecutionResultNode childNode = children.get(key);
                        String resultKey = childNode.getMergedField().getResultKey();
                        newChildren.put(resultKey, childNode);
                    }
                    ObjectExecutionResultNode newNode;
                    if (node instanceof RootExecutionResultNode) {
                        newNode = new RootExecutionResultNode(newChildren);
                    } else {
                        newNode = new ObjectExecutionResultNode(node.getFetchedValueAnalysis(), newChildren);
                    }
                    return TreeTransformerUtil.changeNode(context, newNode);
                }
                return TraversalControl.CONTINUE;
            }
        });

        return newRoot;

    }


    private ListExecutionResultNode mapListExecutionResultNode(ListExecutionResultNode resultNode, GraphQLSchema overallSchema, Map<Field, FieldTransformation> transformationMap) {
        FetchedValueAnalysis fetchedValueAnalysis = fetchedAnalysisMapper.mapFetchedValueAnalysis(resultNode.getFetchedValueAnalysis(), overallSchema, transformationMap);
        return new ListExecutionResultNode(fetchedValueAnalysis, resultNode.getChildren());
    }

    private ObjectExecutionResultNode mapObjectResultNode(ObjectExecutionResultNode objectResultNode, GraphQLSchema overallSchema, Map<Field, FieldTransformation> transformationMap) {
        FetchedValueAnalysis fetchedValueAnalysis = fetchedAnalysisMapper.mapFetchedValueAnalysis(objectResultNode.getFetchedValueAnalysis(), overallSchema, transformationMap);
        return new ObjectExecutionResultNode(fetchedValueAnalysis, objectResultNode.getChildrenMap());
    }

    private RootExecutionResultNode mapRootResultNode(RootExecutionResultNode resultNode, Map<Field, FieldTransformation> transformationMap) {
        return resultNode;
    }

    private LeafExecutionResultNode mapLeafResultNode(LeafExecutionResultNode leafExecutionResultNode,
                                                      GraphQLSchema overallSchema,
                                                      Map<Field, FieldTransformation> transformationMap) {
        //TODO: handle hydration results somehow
        FetchedValueAnalysis fetchedValueAnalysis = fetchedAnalysisMapper.mapFetchedValueAnalysis(leafExecutionResultNode.getFetchedValueAnalysis(), overallSchema, transformationMap);
        return new LeafExecutionResultNode(fetchedValueAnalysis, leafExecutionResultNode.getNonNullableFieldWasNullException());
    }





}
