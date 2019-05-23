package graphql.nadel.engine;

import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.language.Field;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.schema.GraphQLSchema;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;
import graphql.util.TreeTransformerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static graphql.Assert.assertNotNull;
import static graphql.nadel.engine.transformation.FieldTransformation.NADEL_FIELD_ID;
import static java.util.Collections.singletonMap;

public class ServiceResultNodesToOverallResult {

    FetchedValueAnalysisMapper fetchedValueAnalysisMapper = new FetchedValueAnalysisMapper();
    ExecutionStepInfoMapper executionStepInfoMapper = new ExecutionStepInfoMapper();


    @SuppressWarnings("UnnecessaryLocalVariable")
    public ExecutionResultNode convert(ExecutionResultNode resultNode, GraphQLSchema overallSchema, ExecutionStepInfo rootStepInfo, Map<String, FieldTransformation> transformationMap, Map<String, String> typeRenameMappings) {
        return convert(resultNode, overallSchema, rootStepInfo, false, false, transformationMap, typeRenameMappings);
    }

    public ExecutionResultNode convert(ExecutionResultNode resultNode,
                                       GraphQLSchema overallSchema,
                                       ExecutionStepInfo rootStepInfo,
                                       boolean isHydrationTransformation,
                                       boolean batched,
                                       Map<String, FieldTransformation> transformationMap,
                                       Map<String, String> typeRenameMappings) {
        try {
            ResultNodesTransformer resultNodesTransformer = new ResultNodesTransformer();

            Map<Class<?>, Object> rootVars = singletonMap(ExecutionStepInfo.class, rootStepInfo);

            ExecutionResultNode newRoot = resultNodesTransformer.transform(resultNode, new TraverserVisitorStub<ExecutionResultNode>() {
                @Override
                public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {

                    ExecutionResultNode node = context.thisNode();
                    ExecutionStepInfo parentStepInfo = context.getVarFromParents(ExecutionStepInfo.class);

                    if (node instanceof RootExecutionResultNode) {
                        ExecutionResultNode convertedNode = mapRootResultNode((RootExecutionResultNode) node);
                        return TreeTransformerUtil.changeNode(context, convertedNode);
                    }

                    UnapplyEnvironment unapplyEnvironment = new UnapplyEnvironment(
                            context,
                            parentStepInfo,
                            isHydrationTransformation,
                            batched, typeRenameMappings,
                            overallSchema);

                    TraversalControl traversalControl = TraversalControl.CONTINUE;
                    List<FieldTransformation> transformations = getTransformations(node.getMergedField(), transformationMap);
                    if (transformations.size() == 0) {
                        defaultMapping(node, unapplyEnvironment);
                    } else {
                        FieldTransformation transformation = transformations.get(0);
                        traversalControl = assertNotNull(transformation.unapplyResultNode(node, transformations, unapplyEnvironment));
                    }
                    ExecutionResultNode convertedNode = context.thisNode();
                    if (!(convertedNode instanceof LeafExecutionResultNode)) {
                        setExecutionInfo(context, convertedNode);
                    }
                    return traversalControl;
                }

            }, rootVars);
            return newRoot;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void defaultMapping(ExecutionResultNode node, UnapplyEnvironment environment) {
        FetchedValueAnalysis originalFetchAnalysis = node.getFetchedValueAnalysis();

        BiFunction<ExecutionStepInfo, UnapplyEnvironment, ExecutionStepInfo> esiMapper = (esi, env) -> executionStepInfoMapper.mapExecutionStepInfo(esi, env);
        FetchedValueAnalysis mappedFetchedValueAnalysis = fetchedValueAnalysisMapper.mapFetchedValueAnalysis(originalFetchAnalysis, environment, esiMapper);
        TreeTransformerUtil.changeNode(environment.context, node.withNewFetchedValueAnalysis(mappedFetchedValueAnalysis));
    }

    private List<FieldTransformation> getTransformations(MergedField mergedField, Map<String, FieldTransformation> transformationMap) {
        List<FieldTransformation> transformations = new ArrayList<>();
        for (Field field : mergedField.getFields()) {
            String fieldId = field.getAdditionalData().get(NADEL_FIELD_ID);
            if (fieldId == null) {
                continue;
            }
            FieldTransformation fieldTransformation = transformationMap.get(fieldId);
            if (fieldTransformation == null) {
                continue;
            }
            transformations.add(fieldTransformation);
        }
        return transformations;
    }

    private void setExecutionInfo(TraverserContext<ExecutionResultNode> context, ExecutionResultNode resultNode) {
        FetchedValueAnalysis fva = resultNode.getFetchedValueAnalysis();
        ExecutionStepInfo stepInfo = fva.getExecutionStepInfo();
        context.setVar(ExecutionStepInfo.class, stepInfo);
    }

    private RootExecutionResultNode mapRootResultNode(RootExecutionResultNode resultNode) {
        return new RootExecutionResultNode(resultNode.getChildren(), resultNode.getErrors());
    }


}
