package graphql.nadel.engine;

import graphql.Assert;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.nextgen.FetchedValueAnalysis;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.LeafExecutionResultNode;
import graphql.execution.nextgen.result.RootExecutionResultNode;
import graphql.language.AbstractNode;
import graphql.language.Field;
import graphql.nadel.Tuples;
import graphql.nadel.TuplesTwo;
import graphql.nadel.engine.transformation.FieldRenameTransformation;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.schema.GraphQLSchema;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;
import graphql.util.TreeTransformerUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertTrue;
import static graphql.nadel.engine.StrategyUtil.changeFieldInResultNode;
import static graphql.util.FpKit.groupingBy;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

public class ServiceResultNodesToOverallResult {

    FetchedValueAnalysisMapper fetchedValueAnalysisMapper = new FetchedValueAnalysisMapper();
    ExecutionStepInfoMapper executionStepInfoMapper = new ExecutionStepInfoMapper();

    ResultNodesTransformer resultNodesTransformer = new ResultNodesTransformer();


    @SuppressWarnings("UnnecessaryLocalVariable")
    public ExecutionResultNode convert(ExecutionResultNode resultNode, GraphQLSchema overallSchema, ExecutionStepInfo rootStepInfo, Map<String, FieldTransformation> transformationMap, Map<String, String> typeRenameMappings) {
        return convertImpl(resultNode, overallSchema, rootStepInfo, false, false, transformationMap, typeRenameMappings, false);
    }

    public ExecutionResultNode convert(ExecutionResultNode root,
                                       GraphQLSchema overallSchema,
                                       ExecutionStepInfo rootStepInfo,
                                       boolean isHydrationTransformation,
                                       boolean batched,
                                       Map<String, FieldTransformation> transformationMap,
                                       Map<String, String> typeRenameMappings) {

        return convertImpl(root, overallSchema, rootStepInfo, isHydrationTransformation, batched, transformationMap, typeRenameMappings, false);
    }

    private ExecutionResultNode convertImpl(ExecutionResultNode root,
                                            GraphQLSchema overallSchema,
                                            ExecutionStepInfo rootStepInfo,
                                            boolean isHydrationTransformation,
                                            boolean batched,
                                            Map<String, FieldTransformation> transformationMap,
                                            Map<String, String> typeRenameMappings,
                                            boolean onlyChildren) {

        Map<Class<?>, Object> rootVars = singletonMap(ExecutionStepInfo.class, rootStepInfo);
        ExecutionResultNode newRoot = resultNodesTransformer.transform(root, new TraverserVisitorStub<ExecutionResultNode>() {
            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                ExecutionResultNode node = context.thisNode();
                if (onlyChildren && node == root) {
                    return TraversalControl.CONTINUE;
                }
                ExecutionStepInfo parentStepInfo = context.getVarFromParents(ExecutionStepInfo.class);

                if (node instanceof RootExecutionResultNode) {
                    ExecutionResultNode convertedNode = mapRootResultNode((RootExecutionResultNode) node);
                    return TreeTransformerUtil.changeNode(context, convertedNode);
                }

                TraversalControl traversalControl = TraversalControl.CONTINUE;
                TuplesTwo<Set<FieldTransformation>, List<Field>> transformationsAndNotTransformedFields =
                        getTransformationsAndNotTransformedFields(node.getMergedField(), transformationMap);
                List<FieldTransformation> transformations = new ArrayList<>(transformationsAndNotTransformedFields.getT1());
                List<Field> notTransformedFields = transformationsAndNotTransformedFields.getT2();

                UnapplyEnvironment unapplyEnvironment = new UnapplyEnvironment(
                        parentStepInfo,
                        isHydrationTransformation,
                        batched,
                        typeRenameMappings,
                        overallSchema,
                        notTransformedFields
                );
                if (transformations.size() == 0) {
                    mapAndChangeNode(node, unapplyEnvironment, context);
                } else {
                    traversalControl = unapplyTransformations(node, transformations, unapplyEnvironment, transformationMap, context);
                }
                ExecutionResultNode convertedNode = context.thisNode();
                if (!(convertedNode instanceof LeafExecutionResultNode)) {
                    setExecutionInfo(context, convertedNode);
                }
                return traversalControl;
            }

        }, rootVars);
        return newRoot;

    }


    private TraversalControl unapplyTransformations(ExecutionResultNode node,
                                                    List<FieldTransformation> transformations,
                                                    UnapplyEnvironment unapplyEnvironment,
                                                    Map<String, FieldTransformation> transformationMap,
                                                    TraverserContext<ExecutionResultNode> context) {

        TraversalControl traversalControl;

        FieldTransformation transformation = transformations.get(0);

        if (transformation instanceof HydrationTransformation) {
            traversalControl = unapplyHydration(node, transformations, unapplyEnvironment, transformationMap, transformation, context);
        } else if (transformation instanceof FieldRenameTransformation) {
            traversalControl = unapplyFieldRename(node, transformations, unapplyEnvironment, transformationMap, context);
        } else {
            return Assert.assertShouldNeverHappen("Unexpected transformation type " + transformation);
        }
        return traversalControl;
    }

    private TraversalControl unapplyFieldRename(ExecutionResultNode node,
                                                List<FieldTransformation> transformations,
                                                UnapplyEnvironment unapplyEnvironment,
                                                Map<String, FieldTransformation> transformationMap,
                                                TraverserContext<ExecutionResultNode> context) {
        Map<AbstractNode, List<FieldTransformation>> transformationByDefinition = groupingBy(transformations, FieldTransformation::getDefinition);

        TuplesTwo<ExecutionResultNode, Map<AbstractNode, ExecutionResultNode>> splittedNodes = splitTreeByTransformationDefinition(node, transformationMap);
        ExecutionResultNode notTransformedTree = splittedNodes.getT1();
        Map<AbstractNode, ExecutionResultNode> nodesWithTransformedFields = splittedNodes.getT2();

        List<FieldTransformation.UnapplyResult> unapplyResults = new ArrayList<>();
        for (AbstractNode definition : nodesWithTransformedFields.keySet()) {
            List<FieldTransformation> transformationsForDefinition = transformationByDefinition.get(definition);
            FieldTransformation.UnapplyResult unapplyResult = transformationsForDefinition.get(0).unapplyResultNode(nodesWithTransformedFields.get(definition), transformationsForDefinition, unapplyEnvironment);
            unapplyResults.add(unapplyResult);
        }
        boolean first = true;
        if (notTransformedTree != null) {
            mapAndChangeNode(notTransformedTree, unapplyEnvironment, context);
            ExecutionResultNode mappedNode = mapNode(node, unapplyEnvironment, context);
            TreeTransformerUtil.changeNode(context, mappedNode);
            first = false;
        }


        for (FieldTransformation.UnapplyResult unapplyResult : unapplyResults) {
            ExecutionResultNode transformedResult;
            if (unapplyResult.getTraversalControl() != TraversalControl.CONTINUE) {
                transformedResult = unapplyResult.getNode();
            } else {
                ExecutionResultNode unapplyResultNode = unapplyResult.getNode();
                transformedResult = convertImpl(unapplyResultNode,
                        unapplyEnvironment.overallSchema,
                        unapplyEnvironment.parentExecutionStepInfo,
                        unapplyEnvironment.isHydrationTransformation,
                        unapplyEnvironment.batched,
                        transformationMap,
                        unapplyEnvironment.typeRenameMappings,
                        true);
            }
            if (first) {
                TreeTransformerUtil.changeNode(context, transformedResult);
                first = false;
                continue;
            }
            TreeTransformerUtil.insertAfter(context, transformedResult);
        }
        return TraversalControl.CONTINUE;
    }

    private TraversalControl unapplyHydration(ExecutionResultNode node,
                                              List<FieldTransformation> transformations,
                                              UnapplyEnvironment unapplyEnvironment,
                                              Map<String, FieldTransformation> transformationMap,
                                              FieldTransformation transformation,
                                              TraverserContext<ExecutionResultNode> context
    ) {
        TuplesTwo<ExecutionResultNode, Map<AbstractNode, ExecutionResultNode>> splittedNodes = splitTreeByTransformationDefinition(node, transformationMap);
        ExecutionResultNode withoutTransformedFields = splittedNodes.getT1();
        assertTrue(splittedNodes.getT2().size() == 1, "only one split tree expected atm");
        ExecutionResultNode nodesWithTransformedFields = graphql.nadel.util.FpKit.getSingleMapValue(splittedNodes.getT2());

        FieldTransformation.UnapplyResult unapplyResult = transformation.unapplyResultNode(nodesWithTransformedFields, transformations, unapplyEnvironment);

        if (withoutTransformedFields != null) {
            mapAndChangeNode(withoutTransformedFields, unapplyEnvironment, context);
            TreeTransformerUtil.insertAfter(context, unapplyResult.getNode());
            return TraversalControl.CONTINUE;
        } else {
            TreeTransformerUtil.changeNode(context, unapplyResult.getNode());
            return unapplyResult.getTraversalControl();
        }
    }


    private TuplesTwo<ExecutionResultNode, Map<AbstractNode, ExecutionResultNode>> splitTreeByTransformationDefinition(ExecutionResultNode executionResultNode,
                                                                                                                       Map<String, FieldTransformation> transformationMap) {
        if (executionResultNode instanceof RootExecutionResultNode) {
            return Tuples.of(executionResultNode, emptyMap());
        }

        Map<AbstractNode, Set<String>> idsByTransformationDefinition = new LinkedHashMap<>();
        List<Field> fields = executionResultNode.getMergedField().getFields();
        for (Field field : fields) {
            List<String> fieldIds = FieldMetadataUtil.getRootOfTransformationIds(field);
            for (String fieldId : fieldIds) {
                FieldTransformation fieldTransformation = assertNotNull(transformationMap.get(fieldId));
                AbstractNode definition = fieldTransformation.getDefinition();
                idsByTransformationDefinition.putIfAbsent(definition, new LinkedHashSet<>());
                idsByTransformationDefinition.get(definition).add(fieldId);
            }
        }
        Map<AbstractNode, ExecutionResultNode> treesByDefinition = new LinkedHashMap<>();
        for (AbstractNode definition : idsByTransformationDefinition.keySet()) {
            Set<String> ids = idsByTransformationDefinition.get(definition);
            treesByDefinition.put(definition, nodesWithFieldId(executionResultNode, ids));
        }
        ExecutionResultNode treeWithout = nodesWithFieldId(executionResultNode, null);
        return Tuples.of(treeWithout, treesByDefinition);
    }

    private ExecutionResultNode nodesWithFieldId(ExecutionResultNode executionResultNode, Set<String> ids) {
        return resultNodesTransformer.transform(executionResultNode, new TraverserVisitorStub<ExecutionResultNode>() {

            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                ExecutionResultNode node = context.thisNode();
                List<Field> fieldsWithId;
                if (ids == null) {
                    fieldsWithId = getFieldsWithoutNadelId(node);
                } else {
                    fieldsWithId = getFieldsWithNadelId(node, ids);
                }

                if (fieldsWithId.size() == 0) {
                    return TreeTransformerUtil.deleteNode(context);
                }
                MergedField mergedField = MergedField.newMergedField(fieldsWithId).build();
                ExecutionResultNode changedNode = changeFieldInResultNode(node, mergedField);
                return TreeTransformerUtil.changeNode(context, changedNode);
            }
        });


    }


    private List<Field> getFieldsWithoutNadelId(ExecutionResultNode node) {
        return node.getMergedField().getFields().stream().filter(field -> FieldMetadataUtil.getFieldIds(field).size() == 0).collect(Collectors.toList());
    }

    private List<Field> getFieldsWithNadelId(ExecutionResultNode node, Set<String> ids) {
        return node.getMergedField().getFields().stream().filter(field -> {
            List<String> fieldIds = FieldMetadataUtil.getFieldIds(field);
            return fieldIds.containsAll(ids);
        }).collect(Collectors.toList());
    }

    private ExecutionResultNode mapNode(ExecutionResultNode node, UnapplyEnvironment environment, TraverserContext<ExecutionResultNode> context) {
        FetchedValueAnalysis originalFetchAnalysis = node.getFetchedValueAnalysis();

        BiFunction<ExecutionStepInfo, UnapplyEnvironment, ExecutionStepInfo> esiMapper = (esi, env) -> executionStepInfoMapper.mapExecutionStepInfo(esi, env);
        FetchedValueAnalysis mappedFetchedValueAnalysis = fetchedValueAnalysisMapper.mapFetchedValueAnalysis(originalFetchAnalysis, environment, esiMapper);
        return node.withNewFetchedValueAnalysis(mappedFetchedValueAnalysis);
    }

    private void mapAndChangeNode(ExecutionResultNode node, UnapplyEnvironment environment, TraverserContext<ExecutionResultNode> context) {
        ExecutionResultNode mappedNode = mapNode(node, environment, context);
        TreeTransformerUtil.changeNode(context, mappedNode);
    }

    private TuplesTwo<Set<FieldTransformation>, List<Field>> getTransformationsAndNotTransformedFields(MergedField mergedField, Map<String, FieldTransformation> transformationMap) {
        Set<FieldTransformation> transformations = new LinkedHashSet<>();
        List<Field> notTransformedFields = new ArrayList<>();
        for (Field field : mergedField.getFields()) {
            List<String> fieldIds = FieldMetadataUtil.getRootOfTransformationIds(field);
            if (fieldIds.size() == 0) {
                notTransformedFields.add(field);
                continue;
            }
            for (String fieldId : fieldIds) {
                FieldTransformation fieldTransformation = transformationMap.get(fieldId);
                transformations.add(fieldTransformation);
            }
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
