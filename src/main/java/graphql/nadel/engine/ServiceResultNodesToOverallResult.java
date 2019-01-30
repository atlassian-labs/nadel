package graphql.nadel.engine;

import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.execution.nextgen.result.ListExecutionResultNode;
import graphql.execution.nextgen.result.ObjectExecutionResultNode;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.language.Field;
import graphql.nadel.engine.transformation.FieldMappingTransformation;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;
import graphql.util.TreeTransformerUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static graphql.Assert.assertShouldNeverHappen;
import static graphql.Assert.assertTrue;

public class ServiceResultNodesToOverallResult {


    //TODO: the return type is not really ready to return hydration results, which can be used as input for new queries
    public RootExecutionResultNode convert(RootExecutionResultNode resultNode, GraphQLSchema overallSchema, Map<Field, FieldTransformation> transformationMap) {
        ResultNodesTransformer resultNodesTransformer = new ResultNodesTransformer();

        return (RootExecutionResultNode) resultNodesTransformer.transform(resultNode, new TraverserVisitorStub<ExecutionResultNode>() {
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

            @Override
            public TraversalControl leave(TraverserContext<ExecutionResultNode> context) {
                ExecutionResultNode node = context.thisNode();
                if (node instanceof ObjectExecutionResultNode) {
                    Map<String, ExecutionResultNode> newChildren = new LinkedHashMap<>();
                    Map<String, List<TraverserContext<ExecutionResultNode>>> childrenContexts = context.getChildrenContexts();
                    for (String key : childrenContexts.keySet()) {
                        List<TraverserContext<ExecutionResultNode>> childContext = childrenContexts.get(key);
                        assertTrue(childContext.size() == 1, "unexpected children count");
                        ExecutionResultNode childNode = childContext.get(0).thisNode();
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
                return super.leave(context);
            }
        });
    }

    private ListExecutionResultNode mapListExecutionResultNode(ListExecutionResultNode resultNode, GraphQLSchema overallSchema, Map<Field, FieldTransformation> transformationMap) {
        FetchedValueAnalysis fetchedValueAnalysis = mapFetchedValueAnalysis(resultNode.getFetchedValueAnalysis(), overallSchema, transformationMap);
        return new ListExecutionResultNode(fetchedValueAnalysis, resultNode.getChildren());
    }

    private ObjectExecutionResultNode mapObjectResultNode(ObjectExecutionResultNode objectResultNode, GraphQLSchema overallSchema, Map<Field, FieldTransformation> transformationMap) {
        FetchedValueAnalysis fetchedValueAnalysis = mapFetchedValueAnalysis(objectResultNode.getFetchedValueAnalysis(), overallSchema, transformationMap);
        return new ObjectExecutionResultNode(fetchedValueAnalysis, objectResultNode.getChildrenMap());
    }

    private RootExecutionResultNode mapRootResultNode(RootExecutionResultNode resultNode, Map<Field, FieldTransformation> transformationMap) {
        return resultNode;
    }

    private LeafExecutionResultNode mapLeafResultNode(LeafExecutionResultNode leafExecutionResultNode, GraphQLSchema overallSchema, Map<Field, FieldTransformation> transformationMap) {
        //TODO: handle hydration results somehow
        FetchedValueAnalysis fetchedValueAnalysis = mapFetchedValueAnalysis(leafExecutionResultNode.getFetchedValueAnalysis(), overallSchema, transformationMap);
        return new LeafExecutionResultNode(fetchedValueAnalysis, leafExecutionResultNode.getNonNullableFieldWasNullException());
    }

    private FetchedValueAnalysis mapFetchedValueAnalysis(FetchedValueAnalysis fetchedValueAnalysis, GraphQLSchema overallSchema, Map<Field, FieldTransformation> transformationMap) {
        ExecutionStepInfo mappedExecutionStepInfo = mapExecutionStepInfo(fetchedValueAnalysis.getExecutionStepInfo(), overallSchema, transformationMap);
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

    private ExecutionStepInfo mapExecutionStepInfo(ExecutionStepInfo executionStepInfo, GraphQLSchema overallSchema, Map<Field, FieldTransformation> transformationMap) {
        //TODO: handle __typename
        MergedField mergedField = executionStepInfo.getField();
        if (transformationMap.containsKey(mergedField.getSingleField())) {
            mergedField = unapplyTransformation(transformationMap.get(mergedField.getSingleField()), mergedField);
        }
        GraphQLOutputType fieldType = executionStepInfo.getType();
        GraphQLObjectType fieldContainer = executionStepInfo.getFieldContainer();
        GraphQLObjectType mappedFieldContainer = (GraphQLObjectType) overallSchema.getType(fieldContainer.getName());
        //TODO: the line below is not correct as it does not work list or non null types (since fieldType#getName will be null in that case)
        GraphQLOutputType mappedFieldType = (GraphQLOutputType) overallSchema.getType(fieldType.getName());
        GraphQLFieldDefinition fieldDefinition = executionStepInfo.getFieldDefinition();
        GraphQLFieldDefinition mappedFieldDefinition = mappedFieldContainer.getFieldDefinition(fieldDefinition.getName());

        // TODO: map path

        MergedField finalMergedField = mergedField;
        return executionStepInfo.transform(builder -> builder
                .field(finalMergedField)
                .type(mappedFieldType)
                .fieldContainer(mappedFieldContainer)
                .fieldDefinition(mappedFieldDefinition));

    }

    private MergedField unapplyTransformation(FieldTransformation fieldTransformation, MergedField mergedField) {
        if (fieldTransformation instanceof FieldMappingTransformation) {
            String originalName = ((FieldMappingTransformation) fieldTransformation).getOriginalName();
            List<Field> fields = mergedField
                    .getFields()
                    .stream()
                    .map(field -> field.transform(builder -> builder.name(originalName)))
                    .collect(Collectors.toList());
            return MergedField.newMergedField(fields).build();
        }
        return mergedField;
    }


}
