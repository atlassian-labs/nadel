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
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.schema.GraphQLSchema;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;
import graphql.util.TreeTransformerUtil;

import java.util.Map;

import static graphql.Assert.assertShouldNeverHappen;
import static graphql.nadel.engine.StrategyUtil.changeFieldInResultNode;
import static java.util.Collections.singletonMap;

public class ServiceResultNodesToOverallResult {

    FetchedAnalysisMapper fetchedAnalysisMapper = new FetchedAnalysisMapper();


    //TODO: the return type is not really ready to return hydration results, which can be used as input for new queries
    @SuppressWarnings("UnnecessaryLocalVariable")
    public ExecutionResultNode convert(ExecutionResultNode resultNode, GraphQLSchema overallSchema, ExecutionStepInfo rootStepInfo, Map<Field, FieldTransformation> transformationMap, Map<String, String> typeRenameMappings) {
        return convert(resultNode, overallSchema, rootStepInfo, false, false, transformationMap, typeRenameMappings);
    }

    public ExecutionResultNode convert(ExecutionResultNode resultNode, GraphQLSchema overallSchema, ExecutionStepInfo rootStepInfo, boolean isHydrationTransformation, boolean batched, Map<Field, FieldTransformation> transformationMap, Map<String, String> typeRenameMappings) {
        try {
            ResultNodesTransformer resultNodesTransformer = new ResultNodesTransformer();

            Map<Class<?>, Object> rootVars = singletonMap(ExecutionStepInfo.class, rootStepInfo);

            ExecutionResultNode newRoot = resultNodesTransformer.transform(resultNode, new TraverserVisitorStub<ExecutionResultNode>() {
                @Override
                public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                    ExecutionResultNode node = context.thisNode();
                    ExecutionResultNode convertedNode;
                    if (node instanceof RootExecutionResultNode) {
                        convertedNode = mapRootResultNode((RootExecutionResultNode) node);
                    } else if (node instanceof ObjectExecutionResultNode) {
                        ExecutionStepInfo parentStepInfo = context.getVarFromParents(ExecutionStepInfo.class);
                        ObjectExecutionResultNode objectResultNode = (ObjectExecutionResultNode) node;
                        convertedNode = mapObjectResultNode(objectResultNode, overallSchema, parentStepInfo, isHydrationTransformation, batched, transformationMap, typeRenameMappings);
                        setExecutionInfo(context, convertedNode);
                    } else if (node instanceof ListExecutionResultNode) {
                        ExecutionStepInfo parentStepInfo = context.getVarFromParents(ExecutionStepInfo.class);
                        ListExecutionResultNode listExecutionResultNode = (ListExecutionResultNode) node;
                        convertedNode = mapListExecutionResultNode(listExecutionResultNode, overallSchema, parentStepInfo, isHydrationTransformation, batched, transformationMap, typeRenameMappings);
                        setExecutionInfo(context, convertedNode);
                    } else if (node instanceof LeafExecutionResultNode) {
                        ExecutionStepInfo parentStepInfo = context.getVarFromParents(ExecutionStepInfo.class);
                        LeafExecutionResultNode leafExecutionResultNode = (LeafExecutionResultNode) node;
                        convertedNode = mapLeafResultNode(leafExecutionResultNode, overallSchema, parentStepInfo, isHydrationTransformation, batched, transformationMap, typeRenameMappings);
                    } else {
                        return assertShouldNeverHappen();
                    }
                    return TreeTransformerUtil.changeNode(context, convertedNode);
                }

            }, rootVars);
            return newRoot;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setExecutionInfo(TraverserContext<ExecutionResultNode> context, ExecutionResultNode resultNode) {
        FetchedValueAnalysis fva = resultNode.getFetchedValueAnalysis();
        ExecutionStepInfo stepInfo = fva.getExecutionStepInfo();
        context.setVar(ExecutionStepInfo.class, stepInfo);
    }

    private RootExecutionResultNode mapRootResultNode(RootExecutionResultNode resultNode) {
        return new RootExecutionResultNode(resultNode.getChildren(), resultNode.getErrors());
    }

    private ListExecutionResultNode mapListExecutionResultNode(ListExecutionResultNode resultNode,
                                                               GraphQLSchema overallSchema,
                                                               ExecutionStepInfo parentExecutionStepInfo,
                                                               boolean isHydrationTransformation,
                                                               boolean batched,
                                                               Map<Field, FieldTransformation> transformationMap,
                                                               Map<String, String> typeRenameMappings) {
        FetchedValueAnalysis fetchedValueAnalysis = fetchedAnalysisMapper.mapFetchedValueAnalysis(
                resultNode.getFetchedValueAnalysis(), overallSchema, parentExecutionStepInfo, isHydrationTransformation, batched, transformationMap, typeRenameMappings);
        return new ListExecutionResultNode(fetchedValueAnalysis, resultNode.getChildren());
    }

    private ObjectExecutionResultNode mapObjectResultNode(ObjectExecutionResultNode objectResultNode,
                                                          GraphQLSchema overallSchema,
                                                          ExecutionStepInfo parentExecutionStepInfo,
                                                          boolean isHydrationTransformation,
                                                          boolean batched,
                                                          Map<Field, FieldTransformation> transformationMap,
                                                          Map<String, String> typeRenameMappings) {
        FetchedValueAnalysis originalFetchAnalysis = objectResultNode.getFetchedValueAnalysis();
        MergedField originalField = originalFetchAnalysis.getExecutionStepInfo().getField();
        FetchedValueAnalysis fetchedValueAnalysis = fetchedAnalysisMapper.mapFetchedValueAnalysis(
                originalFetchAnalysis, overallSchema, parentExecutionStepInfo, isHydrationTransformation, batched, transformationMap, typeRenameMappings);

        objectResultNode = new ObjectExecutionResultNode(fetchedValueAnalysis, objectResultNode.getChildren());

        FieldTransformation fieldTransformation = transformationMap.get(originalField.getSingleField());
        if (fieldTransformation != null) {
            objectResultNode = fieldTransformation.unapplyResultNode(objectResultNode);
        }
        return objectResultNode;
    }

    private LeafExecutionResultNode mapLeafResultNode(LeafExecutionResultNode leafExecutionResultNode,
                                                      GraphQLSchema overallSchema,
                                                      ExecutionStepInfo parentExecutionStepInfo,
                                                      boolean isHydrationTransformation,
                                                      boolean batched,
                                                      Map<Field, FieldTransformation> transformationMap,
                                                      Map<String, String> typeRenameMappings) {
        FetchedValueAnalysis originalFetchAnalysis = leafExecutionResultNode.getFetchedValueAnalysis();
        FetchedValueAnalysis fetchedValueAnalysis = fetchedAnalysisMapper.mapFetchedValueAnalysis(
                originalFetchAnalysis, overallSchema, parentExecutionStepInfo, isHydrationTransformation, batched, transformationMap, typeRenameMappings);

        MergedField mergedField = leafExecutionResultNode.getMergedField();
        Field singleField = mergedField.getSingleField();
        FieldTransformation fieldTransformation = transformationMap.get(singleField);

        if (fieldTransformation instanceof HydrationTransformation) {
            HydrationTransformation hydrationTransformation = (HydrationTransformation) fieldTransformation;
            if (fetchedValueAnalysis.isNullValue()) {
                // if the field is null we don't need to create a HydrationInputNode: we only need to fix up the field name
                return (LeafExecutionResultNode) changeFieldInResultNode(leafExecutionResultNode, hydrationTransformation.getOriginalField());

            } else {
                return new HydrationInputNode(hydrationTransformation, fetchedValueAnalysis, leafExecutionResultNode.getNonNullableFieldWasNullException());
            }
        }
        return new LeafExecutionResultNode(fetchedValueAnalysis, leafExecutionResultNode.getNonNullableFieldWasNullException());
    }
}
