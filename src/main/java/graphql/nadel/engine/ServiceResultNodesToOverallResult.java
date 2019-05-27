package graphql.nadel.engine;

import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.language.Field;
import graphql.nadel.NadelTuple2;
import graphql.nadel.Tuples;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.schema.GraphQLSchema;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;
import graphql.util.TreeTransformerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertTrue;
import static graphql.nadel.engine.StrategyUtil.changeFieldInResultNode;
import static graphql.nadel.engine.transformation.FieldTransformation.NADEL_FIELD_ID;
import static java.util.Collections.singletonMap;

public class ServiceResultNodesToOverallResult {

    FetchedValueAnalysisMapper fetchedValueAnalysisMapper = new FetchedValueAnalysisMapper();
    ExecutionStepInfoMapper executionStepInfoMapper = new ExecutionStepInfoMapper();

    ResultNodesTransformer resultNodesTransformer = new ResultNodesTransformer();


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

                    TraversalControl traversalControl = TraversalControl.CONTINUE;
                    NadelTuple2<List<FieldTransformation>, List<Field>> transformationsAndNotTransformedFields =
                            getTransformationsAndNotTransformedFields(node.getMergedField(), transformationMap);
                    List<FieldTransformation> transformations = transformationsAndNotTransformedFields.getT1();
                    List<Field> notTransformedFields = transformationsAndNotTransformedFields.getT2();

                    UnapplyEnvironment unapplyEnvironment = new UnapplyEnvironment(
                            context,
                            parentStepInfo,
                            isHydrationTransformation,
                            batched,
                            typeRenameMappings,
                            overallSchema,
                            notTransformedFields
                    );
                    if (transformations.size() == 0) {
                        defaultMapping(node, unapplyEnvironment);
                    } else {
                        traversalControl = unapplyTransformations(node, transformations, unapplyEnvironment);
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

    private TraversalControl unapplyTransformations(ExecutionResultNode node, List<FieldTransformation> transformations, UnapplyEnvironment unapplyEnvironment) {
        TraversalControl traversalControl;
        FieldTransformation transformation = transformations.get(0);

        //TODO: This is not great that we need to handle Hydrations in a special way here by splitting merged fields up again
        if (transformation instanceof HydrationTransformation) {
            NadelTuple2<ExecutionResultNode, ExecutionResultNode> splittedNodes = splitNodes(node);
            ExecutionResultNode nodesWithTransformedFields = splittedNodes.getT1();
            ExecutionResultNode nodesWithoutTransformedFields = splittedNodes.getT2();
            if (nodesWithoutTransformedFields != null) {
                unapplyEnvironment.treeIsSplit = true;
                defaultMapping(nodesWithoutTransformedFields, unapplyEnvironment);
            }
            traversalControl = assertNotNull(transformation.unapplyResultNode(nodesWithTransformedFields, transformations, unapplyEnvironment));
        } else {
            traversalControl = assertNotNull(transformation.unapplyResultNode(node, transformations, unapplyEnvironment));
        }
        return traversalControl;
    }

    private NadelTuple2<ExecutionResultNode, ExecutionResultNode> splitNodes(ExecutionResultNode executionResultNode) {

        if (executionResultNode instanceof RootExecutionResultNode) {
            return Tuples.of(executionResultNode, null);
        }

        ExecutionResultNode nodesWithId = resultNodesTransformer.transform(executionResultNode, new TraverserVisitorStub<ExecutionResultNode>() {

            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                ExecutionResultNode node = context.thisNode();
                List<Field> fieldsWIthId = getFieldsWithNadelId(node);
                if (fieldsWIthId.size() == 0) {
                    return TreeTransformerUtil.deleteNode(context);
                }

                MergedField mergedField = MergedField.newMergedField(fieldsWIthId).build();
                ExecutionResultNode changedNode = changeFieldInResultNode(node, mergedField);
                return TreeTransformerUtil.changeNode(context, changedNode);
            }
        });

        ExecutionResultNode nodesWithoutId = resultNodesTransformer.transform(executionResultNode, new TraverserVisitorStub<ExecutionResultNode>() {

            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                ExecutionResultNode node = context.thisNode();
                List<Field> fieldsWithoutId = getFieldsWithoutNadelId(node);

                if (fieldsWithoutId.size() == 0) {
                    return TreeTransformerUtil.deleteNode(context);
                }

                MergedField mergedField = MergedField.newMergedField(fieldsWithoutId).build();
                ExecutionResultNode changedNode = changeFieldInResultNode(node, mergedField);
                return TreeTransformerUtil.changeNode(context, changedNode);
            }
        });

        return Tuples.of(nodesWithId, nodesWithoutId);
    }

    private List<Field> getFieldsWithoutNadelId(ExecutionResultNode node) {
        return node.getMergedField().getFields().stream().filter(field -> {
            return !field.getAdditionalData().containsKey(NADEL_FIELD_ID);
        }).collect(Collectors.toList());
    }

    private List<Field> getFieldsWithNadelId(ExecutionResultNode node) {
        return node.getMergedField().getFields().stream().filter(field -> field.getAdditionalData().containsKey(NADEL_FIELD_ID)).collect(Collectors.toList());
    }

    private void defaultMapping(ExecutionResultNode node, UnapplyEnvironment environment) {
        FetchedValueAnalysis originalFetchAnalysis = node.getFetchedValueAnalysis();

        BiFunction<ExecutionStepInfo, UnapplyEnvironment, ExecutionStepInfo> esiMapper = (esi, env) -> executionStepInfoMapper.mapExecutionStepInfo(esi, env);
        FetchedValueAnalysis mappedFetchedValueAnalysis = fetchedValueAnalysisMapper.mapFetchedValueAnalysis(originalFetchAnalysis, environment, esiMapper);
        TreeTransformerUtil.changeNode(environment.context, node.withNewFetchedValueAnalysis(mappedFetchedValueAnalysis));
    }

    private NadelTuple2<List<FieldTransformation>, List<Field>> getTransformationsAndNotTransformedFields(MergedField mergedField, Map<String, FieldTransformation> transformationMap) {
        String prevFieldId = null;
        List<FieldTransformation> transformations = new ArrayList<>();
        List<Field> notTransformedFields = new ArrayList<>();
        for (Field field : mergedField.getFields()) {
            String fieldId = field.getAdditionalData().get(NADEL_FIELD_ID);
            if (fieldId == null) {
                notTransformedFields.add(field);
                continue;
            }
            assertTrue(prevFieldId == null || prevFieldId.equals(fieldId), "expecting same field ids per merged field");
            prevFieldId = fieldId;
            FieldTransformation fieldTransformation = transformationMap.get(fieldId);
            if (fieldTransformation == null) {
                notTransformedFields.add(field);
                continue;
            }
            transformations.add(fieldTransformation);
        }
        return Tuples.of(transformations, notTransformedFields);
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
