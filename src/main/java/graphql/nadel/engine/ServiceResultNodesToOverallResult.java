package graphql.nadel.engine;

import graphql.Assert;
import graphql.GraphQLError;
import graphql.execution.ExecutionId;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.nextgen.result.ResolvedValue;
import graphql.language.AbstractNode;
import graphql.language.Field;
import graphql.nadel.Tuples;
import graphql.nadel.TuplesTwo;
import graphql.nadel.engine.transformation.FieldRenameTransformation;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.nadel.engine.transformation.RemovedFieldData;
import graphql.nadel.engine.transformation.UnapplyResult;
import graphql.nadel.normalized.NormalizedQueryField;
import graphql.nadel.result.ExecutionResultNode;
import graphql.nadel.result.LeafExecutionResultNode;
import graphql.nadel.result.RootExecutionResultNode;
import graphql.schema.GraphQLSchema;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;
import graphql.util.TreeTransformerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertTrue;
import static graphql.nadel.engine.StrategyUtil.changeFieldInResultNode;
import static graphql.util.FpKit.groupingBy;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

public class ServiceResultNodesToOverallResult {

    ExecutionStepInfoMapper executionStepInfoMapper = new ExecutionStepInfoMapper();

    ResolvedValueMapper resolvedValueMapper = new ResolvedValueMapper();

    ResultNodesTransformer resultNodesTransformer = new ResultNodesTransformer();

    private static final Logger log = LoggerFactory.getLogger(ServiceResultNodesToOverallResult.class);


    @SuppressWarnings("UnnecessaryLocalVariable")
    public ExecutionResultNode convert(ExecutionId executionId,
                                       ForkJoinPool forkJoinPool,
                                       ExecutionResultNode resultNode,
                                       GraphQLSchema overallSchema,
                                       ExecutionStepInfo rootStepInfo,
                                       Map<String, FieldTransformation> transformationMap,
                                       Map<String, String> typeRenameMappings,
                                       NadelContext nadelContext,
                                       RemovedFieldData removedFields) {
        return convertImpl(executionId, forkJoinPool, resultNode, overallSchema, rootStepInfo, false, false, transformationMap, typeRenameMappings, false, nadelContext, removedFields);
    }

    public ExecutionResultNode convertChildren(ExecutionId executionId,
                                               ForkJoinPool forkJoinPool,
                                               ExecutionResultNode root,
                                               GraphQLSchema overallSchema,
                                               ExecutionStepInfo rootStepInfo,
                                               boolean isHydrationTransformation,
                                               boolean batched,
                                               Map<String, FieldTransformation> transformationMap,
                                               Map<String, String> typeRenameMappings,
                                               NadelContext nadelContext,
                                               RemovedFieldData removedFields) {
        return convertImpl(executionId, forkJoinPool, root, overallSchema, rootStepInfo, isHydrationTransformation, batched, transformationMap, typeRenameMappings, true, nadelContext, removedFields);
    }

    private ExecutionResultNode convertImpl(ExecutionId executionId,
                                            ForkJoinPool forkJoinPool,
                                            ExecutionResultNode root,
                                            GraphQLSchema overallSchema,
                                            ExecutionStepInfo rootStepInfo,
                                            boolean isHydrationTransformation,
                                            boolean batched,
                                            Map<String, FieldTransformation> transformationMapInput,
                                            Map<String, String> typeRenameMappings,
                                            boolean onlyChildren,
                                            NadelContext nadelContext,
                                            RemovedFieldData removedFields) {

        ConcurrentHashMap<String, FieldTransformation> transformationMap = new ConcurrentHashMap<>(transformationMapInput);

//        long startTime = System.currentTimeMillis();
        final AtomicInteger nodeCount = new AtomicInteger();
        Map<Class<?>, Object> rootVars = singletonMap(ExecutionStepInfo.class, rootStepInfo);
        ExecutionResultNode newRoot = resultNodesTransformer.transformParallel(forkJoinPool, root, new TraverserVisitorStub<ExecutionResultNode>() {
            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                nodeCount.incrementAndGet();
                ExecutionResultNode node = context.thisNode();
                if (onlyChildren && node == root) {
                    return TraversalControl.CONTINUE;
                }
                ExecutionStepInfo parentStepInfo = context.getVarFromParents(ExecutionStepInfo.class);

                if (node instanceof RootExecutionResultNode) {
                    ExecutionResultNode convertedNode = mapRootResultNode((RootExecutionResultNode) node);
                    return TreeTransformerUtil.changeNode(context, convertedNode);
                }
                if (node instanceof LeafExecutionResultNode) {
                    LeafExecutionResultNode leaf = (LeafExecutionResultNode) node;
                    MergedField mergedField = leaf.getMergedField();

                    if (ArtificialFieldUtils.isArtificialField(nadelContext, mergedField)) {
                        return TreeTransformerUtil.deleteNode(context);
                    }
                }

                boolean areRemovedFieldsAdded = false;
                MergedField field = node.getExecutionStepInfo().getField();
                List<NormalizedQueryField> childsForMergedField = removedFields.getChildsForMergedField(field);

//                removedFields.

//                removedFields.get()

//                ExecutionPath path = context.thisNode().getExecutionStepInfo().getPath();
//
//                MergedField parentField = parentStepInfo.getField();
//                if (parentField != null && removedFields.size() > 0) {
//                    // When the current field is hydrated and it has removed sibling nodes
//                    // Adds removedNodes as siblings to the current Node
//                    String hydratedParentId = parentField.getSingleField().getAdditionalData().get("id");
//
//                    List<RemovedFieldData> removedFieldDataList = new ArrayList<>();
//                    if (hydratedParentId != null) {
//                        removedFieldDataList = removedFields.getOrDefault(hydratedParentId, null);
//                    }
//
//                    if (removedFieldDataList != null && !removedFieldDataList.isEmpty()) {
//                        List<ExecutionResultNode> removedNodes = constructRemovedNodes(removedFieldDataList, path);
//                        removedNodes.stream().forEach(removedNode -> TreeTransformerUtil.insertAfter(context, removedNode));
//
//                        removedFields.remove(hydratedParentId);
//                        areRemovedFieldsAdded = true;
//                    }
//                }
//
//                MergedField curField = node.getExecutionStepInfo().getField();
//                if (curField != null && removedFields.size() > 0) {
//                    String id = curField.getSingleField().getAdditionalData().get("id");
//                    List<RemovedFieldData> removedFieldDataList = new ArrayList<>();
//                    if (id != null) {
//                        removedFieldDataList = removedFields.get(id);
//                    }
//
//                    if (removedFieldDataList != null && !removedFieldDataList.isEmpty()) {
//
//                        ExecutionPath curExecutionPath = context.thisNode().getExecutionStepInfo().getPath();
//
////                        if (removedFieldExecutionPath.equals(curExecutionPath)) {
////                            // Edge case: When the current field is hydrated and has been removed, We need to include the removed errors otherwise they are not added to the overallResult
////                            List<GraphQLError> errors = context.thisNode().getErrors();
////                            errors.add(removedFieldDataList.get(0).getGraphQLError());
////                            TreeTransformerUtil.changeNode(context, context.thisNode().withNewErrors(errors));
////                        } else {
//                        // Case: when the current field is parent of removed fields
//                        if (node instanceof ListExecutionResultNode) {
//                            // When the query is batched together
//                            List<ExecutionResultNode> newNodeChildren = new ArrayList<>();
//
//                            for (ExecutionResultNode resultNode : node.getChildren()) {
//                                List<ExecutionResultNode> childrenAndRemovedNodes = new ArrayList<>();
//                                childrenAndRemovedNodes.addAll(resultNode.getChildren());
//                                childrenAndRemovedNodes.addAll(constructRemovedNodes(removedFieldDataList, curExecutionPath));
//
//                                ExecutionStepInfo esi = resultNode.getExecutionStepInfo();
//                                ResolvedValue value = resultNode.getResolvedValue();
//                                List<GraphQLError> errors = resultNode.getErrors();
//
//                                ObjectExecutionResultNode changedNode = new ObjectExecutionResultNode(esi, value, childrenAndRemovedNodes, errors);
//                                newNodeChildren.add(changedNode);
//                            }
//
//                            TreeTransformerUtil.changeNode(context, node.withNewChildren(newNodeChildren));
//                        } else {
//                            List<ExecutionResultNode> childrenAndRemovedNodes = new ArrayList<>();
//                            childrenAndRemovedNodes.addAll(context.thisNode().getChildren());
//                            childrenAndRemovedNodes.addAll(constructRemovedNodes(removedFieldDataList, curExecutionPath));
//
//                            ExecutionStepInfo esi = context.thisNode().getExecutionStepInfo();
//                            ResolvedValue value = context.thisNode().getResolvedValue();
//                            List<GraphQLError> errors = context.thisNode().getErrors();
//
//                            ObjectExecutionResultNode changedNode = new ObjectExecutionResultNode(esi, value, childrenAndRemovedNodes, errors);
//                            TreeTransformerUtil.changeNode(context, (ExecutionResultNode) changedNode);
//                        }
//                    }
//                    removedFields.remove(id);
//                    areRemovedFieldsAdded = true;
//                }
//                }


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
                        overallSchema
                );
                if (transformations.size() == 0) {
                    mapAndChangeNode(node, unapplyEnvironment, context, areRemovedFieldsAdded);
                } else {
                    traversalControl = unapplyTransformations(executionId, forkJoinPool, node, transformations, unapplyEnvironment, transformationMap, context, nadelContext, removedFields);
                }
                ExecutionResultNode convertedNode = context.thisNode();
                if (!(convertedNode instanceof LeafExecutionResultNode)) {
                    setExecutionInfo(context, convertedNode);
                }
                return traversalControl;
            }

        }, rootVars);
//        long elapsedTime = System.currentTimeMillis() - startTime;
//        log.debug("ServiceResultNodesToOverallResult time: {} ms, nodeCount: {}, executionId: {} ", elapsedTime, nodeCount.get(), executionId);
        return newRoot;

    }

//    private List<ExecutionResultNode> createNullResultNode(ExecutionResultNode parent, NormalizedQueryField normalizedQueryField, MergedField field) {
////        List<ExecutionResultNode> executionResultNodes = new ArrayList<>();
////        for (RemovedFieldData removedField : removedFieldDataList) {
//        ResolvedValue resolvedValue = ResolvedValue.newResolvedValue().completedValue(null)
//                .localContext(null)
//                .nullValue(true)
//                .build();
//
//        ExecutionPath path = parent.getExecutionStepInfo().getPath();
//        ExecutionPath executionPath = path.append(ExecutionPath.parse(field.getName()));
//
//        ExecutionStepInfo esi = ExecutionStepInfo.newExecutionStepInfo()
//                .path(executionPath)
//                .type(normalizedQueryField.getFieldDefinition().getType())
//                .field(field)
//                .fieldContainer(normalizedQueryField.getObjectType())
//                .build();
//
////        LeafExecutionResultNode removedNode = new LeafExecutionResultNode(esi, resolvedValue, null, Collections.singletonList(removedField.getGraphQLError()));
////        executionResultNodes.add((ExecutionResultNode) removedNode);
////        return executionResultNodes;
//    }


    private TraversalControl unapplyTransformations(ExecutionId executionId,
                                                    ForkJoinPool forkJoinPool,
                                                    ExecutionResultNode node,
                                                    List<FieldTransformation> transformations,
                                                    UnapplyEnvironment unapplyEnvironment,
                                                    Map<String, FieldTransformation> transformationMap,
                                                    TraverserContext<ExecutionResultNode> context,
                                                    NadelContext nadelContext,
                                                    RemovedFieldData removedFields) {

        TraversalControl traversalControl;

        FieldTransformation transformation = transformations.get(0);

        if (transformation instanceof HydrationTransformation) {
            traversalControl = unapplyHydration(node, transformations, unapplyEnvironment, transformationMap, transformation, context);
        } else if (transformation instanceof FieldRenameTransformation) {
            traversalControl = unapplyFieldRename(executionId, forkJoinPool, node, transformations, unapplyEnvironment, transformationMap, context, nadelContext, removedFields);
        } else {
            return Assert.assertShouldNeverHappen("Unexpected transformation type " + transformation);
        }
        return traversalControl;
    }

    private TraversalControl unapplyFieldRename(ExecutionId executionId,
                                                ForkJoinPool forkJoinPool,
                                                ExecutionResultNode node,
                                                List<FieldTransformation> transformations,
                                                UnapplyEnvironment unapplyEnvironment,
                                                Map<String, FieldTransformation> transformationMap,
                                                TraverserContext<ExecutionResultNode> context,
                                                NadelContext nadelContext,
                                                RemovedFieldData removedFields) {
        Map<AbstractNode, List<FieldTransformation>> transformationByDefinition = groupingBy(transformations, FieldTransformation::getDefinition);

        TuplesTwo<ExecutionResultNode, Map<AbstractNode, ExecutionResultNode>> splittedNodes = splitTreeByTransformationDefinition(node, transformationMap);
        ExecutionResultNode notTransformedTree = splittedNodes.getT1();
        Map<AbstractNode, ExecutionResultNode> nodesWithTransformedFields = splittedNodes.getT2();

        List<UnapplyResult> unapplyResults = new ArrayList<>();
        for (AbstractNode definition : nodesWithTransformedFields.keySet()) {
            List<FieldTransformation> transformationsForDefinition = transformationByDefinition.get(definition);
            UnapplyResult unapplyResult = transformationsForDefinition.get(0).unapplyResultNode(nodesWithTransformedFields.get(definition), transformationsForDefinition, unapplyEnvironment);
            unapplyResults.add(unapplyResult);
        }

        boolean first = true;
        if (notTransformedTree != null) {
            ExecutionResultNode mappedNode = mapNode(node, unapplyEnvironment, context, false);
            mappedNode = convertChildren(executionId,
                    forkJoinPool,
                    mappedNode,
                    unapplyEnvironment.overallSchema,
                    unapplyEnvironment.parentExecutionStepInfo,
                    unapplyEnvironment.isHydrationTransformation,
                    unapplyEnvironment.batched,
                    transformationMap,
                    unapplyEnvironment.typeRenameMappings,
                    nadelContext, removedFields);
            TreeTransformerUtil.changeNode(context, mappedNode);
            first = false;
        }

        for (UnapplyResult unapplyResult : unapplyResults) {
            ExecutionResultNode transformedResult;
            if (unapplyResult.getTraversalControl() != TraversalControl.CONTINUE) {
                transformedResult = unapplyResult.getNode();
            } else {
                ExecutionResultNode unapplyResultNode = unapplyResult.getNode();
                transformedResult = convertChildren(executionId,
                        forkJoinPool,
                        unapplyResultNode,
                        unapplyEnvironment.overallSchema,
                        unapplyResultNode.getExecutionStepInfo(),
                        unapplyEnvironment.isHydrationTransformation,
                        unapplyEnvironment.batched,
                        transformationMap,
                        unapplyEnvironment.typeRenameMappings,
                        nadelContext,
                        removedFields);
            }
            if (first) {
                TreeTransformerUtil.changeNode(context, transformedResult);
                first = false;
            } else {
                TreeTransformerUtil.insertAfter(context, transformedResult);
            }
        }
        return TraversalControl.ABORT;
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

        UnapplyResult unapplyResult = transformation.unapplyResultNode(nodesWithTransformedFields, transformations, unapplyEnvironment);

        if (withoutTransformedFields != null) {
            mapAndChangeNode(withoutTransformedFields, unapplyEnvironment, context, false);
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

    private ExecutionResultNode mapNode(ExecutionResultNode node, UnapplyEnvironment environment, TraverserContext<ExecutionResultNode> context, boolean areRemovedFieldsAdded) {
        ExecutionStepInfo mappedEsi = executionStepInfoMapper.mapExecutionStepInfo(node.getExecutionStepInfo(), environment);
        ResolvedValue mappedResolvedValue = resolvedValueMapper.mapResolvedValue(node, environment);
        if (!areRemovedFieldsAdded) {
            return node.withNewExecutionStepInfo(mappedEsi).withNewResolvedValue(mappedResolvedValue);
        }

        List<GraphQLError> errors = context.thisNode().getErrors();

        if (node instanceof LeafExecutionResultNode) {
            return node.withNewExecutionStepInfo(mappedEsi).withNewResolvedValue(mappedResolvedValue)
                    .withNewErrors(errors);
        }

        List<ExecutionResultNode> children = context.thisNode().getChildren();
        return node.withNewExecutionStepInfo(mappedEsi).withNewResolvedValue(mappedResolvedValue)
                .withNewChildren(children);
    }

    private void mapAndChangeNode(ExecutionResultNode node, UnapplyEnvironment environment, TraverserContext<ExecutionResultNode> context, boolean areRemovedFieldsAdded) {
        ExecutionResultNode mappedNode = mapNode(node, environment, context, areRemovedFieldsAdded);
        TreeTransformerUtil.changeNode(context, mappedNode);
    }

    private TuplesTwo<Set<FieldTransformation>, List<Field>> getTransformationsAndNotTransformedFields(MergedField mergedField,
                                                                                                       Map<String, FieldTransformation> transformationMap) {
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
        ExecutionStepInfo stepInfo = resultNode.getExecutionStepInfo();
        context.setVar(ExecutionStepInfo.class, stepInfo);
    }

    private RootExecutionResultNode mapRootResultNode(RootExecutionResultNode resultNode) {
        return new RootExecutionResultNode(resultNode.getChildren(), resultNode.getErrors(), resultNode.getElapsedTime());
    }


}
