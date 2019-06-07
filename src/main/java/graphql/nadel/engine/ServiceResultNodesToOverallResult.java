package graphql.nadel.engine;

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
                    TuplesTwo<Set<FieldTransformation>, List<Field>> transformationsAndNotTransformedFields =
                            getTransformationsAndNotTransformedFields(node.getMergedField(), transformationMap);
                    List<FieldTransformation> transformations = new ArrayList<>(transformationsAndNotTransformedFields.getT1());
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
                        traversalControl = unapplyTransformations(node, transformations, unapplyEnvironment, transformationMap);
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

    private TraversalControl unapplyTransformations(ExecutionResultNode node, List<FieldTransformation> transformations, UnapplyEnvironment unapplyEnvironment, Map<String, FieldTransformation> transformationMap) {

        TraversalControl traversalControl;

        FieldTransformation transformation = transformations.get(0);

        if (transformation instanceof HydrationTransformation) {
            TuplesTwo<ExecutionResultNode, Map<AbstractNode, ExecutionResultNode>> splittedNodes = splitTreeByTransformationDefinition(node, transformationMap);
            ExecutionResultNode withoutTransformedFields = splittedNodes.getT1();
            assertTrue(splittedNodes.getT2().size() == 1, "only one split tree expected atm");
            ExecutionResultNode nodesWithTransformedFields = graphql.nadel.util.FpKit.getSingleMapValue(splittedNodes.getT2());
            if (withoutTransformedFields != null) {
                unapplyEnvironment.treeIsSplit = true;
                defaultMapping(withoutTransformedFields, unapplyEnvironment);
            }
            traversalControl = assertNotNull(transformation.unapplyResultNode(nodesWithTransformedFields, transformations, unapplyEnvironment));
        } else if (transformation instanceof FieldRenameTransformation) {

            Map<AbstractNode, List<FieldTransformation>> transformationByDefinition = groupingBy(transformations, FieldTransformation::getDefinition);

            TuplesTwo<ExecutionResultNode, Map<AbstractNode, ExecutionResultNode>> splittedNodes = splitTreeByTransformationDefinition(node, transformationMap);
            ExecutionResultNode withoutTransformedFields = splittedNodes.getT1();
            Map<AbstractNode, ExecutionResultNode> nodesWithTransformedFields = splittedNodes.getT2();
            if (withoutTransformedFields != null) {
                unapplyEnvironment.treeIsSplit = true;
                defaultMapping(withoutTransformedFields, unapplyEnvironment);
            }
            traversalControl = TraversalControl.CONTINUE;
            boolean first = true;
            for (AbstractNode definition : nodesWithTransformedFields.keySet()) {
                List<FieldTransformation> transformationsForDefinition = transformationByDefinition.get(definition);
                traversalControl = transformationsForDefinition.get(0).unapplyResultNode(nodesWithTransformedFields.get(definition), transformationsForDefinition, unapplyEnvironment);
                if (first) {
                    unapplyEnvironment.treeIsSplit = true;
                    first = false;
                }
            }
        } else {
            traversalControl = assertNotNull(transformation.unapplyResultNode(node, transformations, unapplyEnvironment));
        }
        return traversalControl;
    }


    private TuplesTwo<ExecutionResultNode, Map<AbstractNode, ExecutionResultNode>> splitTreeByTransformationDefinition(ExecutionResultNode executionResultNode,
                                                                                                                       Map<String, FieldTransformation> transformationMap) {
        if (executionResultNode instanceof RootExecutionResultNode) {
            return Tuples.of(executionResultNode, emptyMap());
        }

        Map<AbstractNode, Set<String>> idsByTransformationDefinition = new LinkedHashMap<>();
        List<Field> fields = executionResultNode.getMergedField().getFields();
        for (Field field : fields) {
            List<String> fieldIds = FieldIdUtil.getFieldIds(field);
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
        return node.getMergedField().getFields().stream().filter(field -> FieldIdUtil.getFieldIds(field).size() == 0).collect(Collectors.toList());
    }

    private List<Field> getFieldsWithNadelId(ExecutionResultNode node, Set<String> ids) {
        return node.getMergedField().getFields().stream().filter(field -> {
            List<String> fieldIds = FieldIdUtil.getFieldIds(field);
            return fieldIds.containsAll(ids);
        }).collect(Collectors.toList());
    }

    private void defaultMapping(ExecutionResultNode node, UnapplyEnvironment environment) {
        FetchedValueAnalysis originalFetchAnalysis = node.getFetchedValueAnalysis();

        BiFunction<ExecutionStepInfo, UnapplyEnvironment, ExecutionStepInfo> esiMapper = (esi, env) -> executionStepInfoMapper.mapExecutionStepInfo(esi, env);
        FetchedValueAnalysis mappedFetchedValueAnalysis = fetchedValueAnalysisMapper.mapFetchedValueAnalysis(originalFetchAnalysis, environment, esiMapper);
        TreeTransformerUtil.changeNode(environment.context, node.withNewFetchedValueAnalysis(mappedFetchedValueAnalysis));
    }

    private TuplesTwo<Set<FieldTransformation>, List<Field>> getTransformationsAndNotTransformedFields(MergedField mergedField, Map<String, FieldTransformation> transformationMap) {
        Set<FieldTransformation> transformations = new LinkedHashSet<>();
        List<Field> notTransformedFields = new ArrayList<>();
        for (Field field : mergedField.getFields()) {
            List<String> fieldIds = FieldIdUtil.getRootOfTransformationIds(field);
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
