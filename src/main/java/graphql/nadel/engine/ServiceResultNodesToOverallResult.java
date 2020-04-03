package graphql.nadel.engine;

import graphql.Assert;
import graphql.GraphQLError;
import graphql.execution.ExecutionId;
import graphql.execution.ExecutionPath;
import graphql.execution.MergedField;
import graphql.execution.nextgen.result.ResolvedValue;
import graphql.language.AbstractNode;
import graphql.language.Field;
import graphql.nadel.Tuples;
import graphql.nadel.TuplesTwo;
import graphql.nadel.engine.transformation.FieldRenameTransformation;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.nadel.engine.transformation.Metadata;
import graphql.nadel.engine.transformation.Metadata.NormalizedFieldAndError;
import graphql.nadel.engine.transformation.UnapplyResult;
import graphql.nadel.normalized.NormalizedQueryField;
import graphql.nadel.normalized.NormalizedQueryFromAst;
import graphql.nadel.result.ExecutionResultNode;
import graphql.nadel.result.LeafExecutionResultNode;
import graphql.nadel.result.ObjectExecutionResultNode;
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
import static graphql.nadel.dsl.NodeId.getId;
import static graphql.nadel.engine.StrategyUtil.changeFieldInResultNode;
import static graphql.util.FpKit.groupingBy;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

public class ServiceResultNodesToOverallResult {

    ExecutionResultNodeMapper executionResultNodeMapper = new ExecutionResultNodeMapper();
    PathMapper pathMapper = new PathMapper();

    ResolvedValueMapper resolvedValueMapper = new ResolvedValueMapper();

    ResultNodesTransformer resultNodesTransformer = new ResultNodesTransformer();

    private static final Logger log = LoggerFactory.getLogger(ServiceResultNodesToOverallResult.class);


    @SuppressWarnings("UnnecessaryLocalVariable")
    public ExecutionResultNode convert(ExecutionId executionId,
                                       ForkJoinPool forkJoinPool,
                                       ExecutionResultNode resultNode,
                                       GraphQLSchema overallSchema,
                                       ExecutionResultNode correctRootNode,
                                       Map<String, FieldTransformation> transformationMap,
                                       Map<String, String> typeRenameMappings,
                                       NadelContext nadelContext,
                                       Metadata metadata) {
        return convertImpl(executionId, forkJoinPool, resultNode, null, overallSchema, correctRootNode, false, false, transformationMap, typeRenameMappings, false, nadelContext, metadata);
    }

    public ExecutionResultNode convertChildren(ExecutionId executionId,
                                               ForkJoinPool forkJoinPool,
                                               ExecutionResultNode root,
                                               NormalizedQueryField normalizedRootField,
                                               GraphQLSchema overallSchema,
                                               ExecutionResultNode correctRootNode,
                                               boolean isHydrationTransformation,
                                               boolean batched,
                                               Map<String, FieldTransformation> transformationMap,
                                               Map<String, String> typeRenameMappings,
                                               NadelContext nadelContext,
                                               Metadata metadata) {
        return convertImpl(executionId, forkJoinPool, root, normalizedRootField, overallSchema, correctRootNode, isHydrationTransformation, batched, transformationMap, typeRenameMappings, true, nadelContext, metadata);
    }

    private ExecutionResultNode convertImpl(ExecutionId executionId,
                                            ForkJoinPool forkJoinPool,
                                            ExecutionResultNode root,
                                            NormalizedQueryField normalizedRootField,
                                            GraphQLSchema overallSchema,
                                            ExecutionResultNode correctRootNode,
                                            boolean isHydrationTransformation,
                                            boolean batched,
                                            Map<String, FieldTransformation> transformationMapInput,
                                            Map<String, String> typeRenameMappings,
                                            boolean onlyChildren,
                                            NadelContext nadelContext,
                                            Metadata metadata) {

        ConcurrentHashMap<String, FieldTransformation> transformationMap = new ConcurrentHashMap<>(transformationMapInput);

//        long startTime = System.currentTimeMillis();
        final AtomicInteger nodeCount = new AtomicInteger();
        ExecutionResultNode newRoot = resultNodesTransformer.transformParallel(forkJoinPool, root, new TraverserVisitorStub<ExecutionResultNode>() {
            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                nodeCount.incrementAndGet();
                ExecutionResultNode node = context.thisNode();

                if (onlyChildren && node == root) {
                    if (root instanceof ObjectExecutionResultNode) {
                        addDeletedChilds(context, (ObjectExecutionResultNode) node, normalizedRootField, nadelContext, metadata);
                    }
                    return TraversalControl.CONTINUE;
                }

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


                TraversalControl traversalControl = TraversalControl.CONTINUE;
                TuplesTwo<Set<FieldTransformation>, List<Field>> transformationsAndNotTransformedFields =
                        getTransformationsAndNotTransformedFields(node, transformationMap, metadata);

                List<FieldTransformation> transformations = new ArrayList<>(transformationsAndNotTransformedFields.getT1());
//                List<Field> notTransformedFields = transformationsAndNotTransformedFields.getT2();

                ExecutionResultNode parentNode = context.getParentContext().originalThisNode() == root ? correctRootNode : context.getParentNode();
                UnapplyEnvironment unapplyEnvironment = new UnapplyEnvironment(
                        parentNode,
                        isHydrationTransformation,
                        batched,
                        typeRenameMappings,
                        overallSchema
                );
                if (transformations.size() == 0) {
                    mapAndChangeNode(node, unapplyEnvironment, context);
                } else {
                    traversalControl = unapplyTransformations(executionId, forkJoinPool, node, transformations, unapplyEnvironment, transformationMap, context, nadelContext, metadata);
                }
                ExecutionResultNode convertedNode = context.thisNode();

                if (convertedNode instanceof ObjectExecutionResultNode) {
                    addDeletedChilds(context, (ObjectExecutionResultNode) convertedNode, null, nadelContext, metadata);
                }


                return traversalControl;
            }

        });
//        long elapsedTime = System.currentTimeMillis() - startTime;
//        log.debug("ServiceResultNodesToOverallResult time: {} ms, nodeCount: {}, executionId: {} ", elapsedTime, nodeCount.get(), executionId);
        return newRoot;

    }

    private void addDeletedChilds(TraverserContext<ExecutionResultNode> context,
                                  ObjectExecutionResultNode resultNode,
                                  NormalizedQueryField normalizedQueryField,
                                  NadelContext nadelContext,
                                  Metadata metadata
    ) {
        if (normalizedQueryField == null) {
            normalizedQueryField = getNormalizedQueryFieldForResultNode(resultNode, nadelContext.getNormalizedOverallQuery());
        }
        List<NormalizedFieldAndError> removedFields = metadata.getRemovedFieldsForParent(normalizedQueryField);
        for (NormalizedFieldAndError normalizedFieldAndError : removedFields) {
            MergedField mergedField = nadelContext.getNormalizedOverallQuery().getMergedFieldByNormalizedFields().get(normalizedFieldAndError.getNormalizedField());
            LeafExecutionResultNode newChild = createRemovedFieldResult(resultNode, mergedField, normalizedFieldAndError.getNormalizedField(), normalizedFieldAndError.getError());
            TreeTransformerUtil.changeNode(context, resultNode.transform(b -> b.addChild(newChild)));
        }
    }

    private LeafExecutionResultNode createRemovedFieldResult(ExecutionResultNode parent,
                                                             MergedField mergedField,
                                                             NormalizedQueryField normalizedQueryField,
                                                             GraphQLError error) {
        ResolvedValue resolvedValue = ResolvedValue.newResolvedValue().completedValue(null)
                .localContext(null)
                .nullValue(true)
                .build();

        ExecutionPath parentPath = parent.getExecutionPath();
        ExecutionPath executionPath = parentPath.segment(normalizedQueryField.getResultKey());

        LeafExecutionResultNode removedNode = LeafExecutionResultNode.newLeafExecutionResultNode()
                .executionPath(executionPath)
                .field(mergedField)
                .objectType(normalizedQueryField.getObjectType())
                .fieldDefinition(normalizedQueryField.getFieldDefinition())
                .resolvedValue(resolvedValue)
                .errors(singletonList(error))
                .build();
        return removedNode;
    }


    private TraversalControl unapplyTransformations(ExecutionId executionId,
                                                    ForkJoinPool forkJoinPool,
                                                    ExecutionResultNode node,
                                                    List<FieldTransformation> transformations,
                                                    UnapplyEnvironment unapplyEnvironment,
                                                    Map<String, FieldTransformation> transformationMap,
                                                    TraverserContext<ExecutionResultNode> context,
                                                    NadelContext nadelContext,
                                                    Metadata metadata) {

        TraversalControl traversalControl;

        FieldTransformation transformation = transformations.get(0);

        if (transformation instanceof HydrationTransformation) {
            traversalControl = unapplyHydration(node, transformations, unapplyEnvironment, transformationMap, transformation, context, metadata);
        } else if (transformation instanceof FieldRenameTransformation) {
            traversalControl = unapplyFieldRename(executionId, forkJoinPool, node, transformations, unapplyEnvironment, transformationMap, context, nadelContext, metadata);
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
                                                Metadata metadata) {
        Map<AbstractNode, List<FieldTransformation>> transformationByDefinition = groupingBy(transformations, FieldTransformation::getDefinition);

        TuplesTwo<ExecutionResultNode, Map<AbstractNode, ExecutionResultNode>> splittedNodes = splitTreeByTransformationDefinition(node, transformationMap, metadata);
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
            ExecutionResultNode mappedNode = mapNode(node, unapplyEnvironment, context);
            mappedNode = convertChildren(executionId,
                    forkJoinPool,
                    mappedNode,
                    null,
                    unapplyEnvironment.overallSchema,
                    unapplyEnvironment.parentNode,
                    unapplyEnvironment.isHydrationTransformation,
                    unapplyEnvironment.batched,
                    transformationMap,
                    unapplyEnvironment.typeRenameMappings,
                    nadelContext,
                    metadata);
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
                        null,
                        unapplyEnvironment.overallSchema,
                        unapplyResultNode,
                        unapplyEnvironment.isHydrationTransformation,
                        unapplyEnvironment.batched,
                        transformationMap,
                        unapplyEnvironment.typeRenameMappings,
                        nadelContext,
                        metadata);
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
                                              TraverserContext<ExecutionResultNode> context,
                                              Metadata metadata
    ) {
        TuplesTwo<ExecutionResultNode, Map<AbstractNode, ExecutionResultNode>> splittedNodes = splitTreeByTransformationDefinition(node, transformationMap, metadata);
        ExecutionResultNode withoutTransformedFields = splittedNodes.getT1();
        assertTrue(splittedNodes.getT2().size() == 1, "only one split tree expected atm");
        ExecutionResultNode nodesWithTransformedFields = graphql.nadel.util.FpKit.getSingleMapValue(splittedNodes.getT2());

        UnapplyResult unapplyResult = transformation.unapplyResultNode(nodesWithTransformedFields, transformations, unapplyEnvironment);

        if (withoutTransformedFields != null) {
            mapAndChangeNode(withoutTransformedFields, unapplyEnvironment, context);
            TreeTransformerUtil.insertAfter(context, unapplyResult.getNode());
            return TraversalControl.CONTINUE;
        } else {
            TreeTransformerUtil.changeNode(context, unapplyResult.getNode());
            return unapplyResult.getTraversalControl();
        }
    }


    private TuplesTwo<ExecutionResultNode, Map<AbstractNode, ExecutionResultNode>> splitTreeByTransformationDefinition(
            ExecutionResultNode executionResultNode,
            Map<String, FieldTransformation> transformationMap,
            Metadata metadata) {
        if (executionResultNode instanceof RootExecutionResultNode) {
            return Tuples.of(executionResultNode, emptyMap());
        }

        Map<AbstractNode, Set<String>> idsByTransformationDefinition = new LinkedHashMap<>();
        List<Field> fields = executionResultNode.getMergedField().getFields();
        for (Field field : fields) {
            List<String> fieldIds = FieldMetadataUtil.getRootOfTransformationIds(field, metadata.getMetadataByFieldId());
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
            treesByDefinition.put(definition, nodesWithFieldId(executionResultNode, ids, metadata));
        }
        ExecutionResultNode treeWithout = nodesWithFieldId(executionResultNode, null, metadata);
        return Tuples.of(treeWithout, treesByDefinition);
    }

    private ExecutionResultNode nodesWithFieldId(ExecutionResultNode executionResultNode, Set<String> ids, Metadata metadata) {
        return resultNodesTransformer.transform(executionResultNode, new TraverserVisitorStub<ExecutionResultNode>() {

            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                ExecutionResultNode node = context.thisNode();
                List<Field> fieldsWithId;
                if (ids == null) {
                    fieldsWithId = getFieldsWithoutNadelId(node, metadata);
                } else {
                    fieldsWithId = getFieldsWithNadelId(node, ids, metadata);
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


    private List<Field> getFieldsWithoutNadelId(ExecutionResultNode node, Metadata metadata) {
        return node.getMergedField().getFields().stream().filter(field -> FieldMetadataUtil.getFieldIds(field, metadata.getMetadataByFieldId()).size() == 0).collect(Collectors.toList());
    }

    private List<Field> getFieldsWithNadelId(ExecutionResultNode node, Set<String> ids, Metadata metadata) {
        return node.getMergedField().getFields().stream().filter(field -> {
            List<String> fieldIds = FieldMetadataUtil.getFieldIds(field, metadata.getMetadataByFieldId());
            return fieldIds.containsAll(ids);
        }).collect(Collectors.toList());
    }

    private ExecutionResultNode mapNode(ExecutionResultNode node, UnapplyEnvironment environment, TraverserContext<ExecutionResultNode> context) {
        ExecutionResultNode mappedNode = executionResultNodeMapper.mapERNFromUnderlyingToOverall(node, environment);
        ResolvedValue mappedResolvedValue = resolvedValueMapper.mapResolvedValue(node, environment);
        ExecutionPath executionPath = pathMapper.mapPath(node.getExecutionPath(), mappedNode.getField(), environment);
        return mappedNode.transform(builder -> builder.resolvedValue(mappedResolvedValue).executionPath(executionPath));
    }

    private void mapAndChangeNode(ExecutionResultNode node, UnapplyEnvironment environment, TraverserContext<ExecutionResultNode> context) {
        ExecutionResultNode mappedNode = mapNode(node, environment, context);
        TreeTransformerUtil.changeNode(context, mappedNode);
    }

    private TuplesTwo<Set<FieldTransformation>, List<Field>> getTransformationsAndNotTransformedFields(
            ExecutionResultNode node,
            Map<String, FieldTransformation> transformationMap,
            Metadata metadata
    ) {
        Set<FieldTransformation> transformations = new LinkedHashSet<>();
        List<Field> notTransformedFields = new ArrayList<>();
        for (Field field : node.getMergedField().getFields()) {
//            System.out.println("processing " + node.getExecutionPath());
//            System.out.println("field id: " + getId(field) + " field: " + field.getName());

            if (node.getExecutionPath().isListSegment()) {
                notTransformedFields.add(field);
                continue;
            }

            List<String> fieldIds = FieldMetadataUtil.getRootOfTransformationIds(field, metadata.getMetadataByFieldId());
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

    private RootExecutionResultNode mapRootResultNode(RootExecutionResultNode resultNode) {
        return RootExecutionResultNode.newRootExecutionResultNode()
                .children(resultNode.getChildren())
                .errors(resultNode.getErrors())
                .elapsedTime(resultNode.getElapsedTime())
                .build();
    }

    private NormalizedQueryField getNormalizedQueryFieldForResultNode(ObjectExecutionResultNode resultNode, NormalizedQueryFromAst normalizedQueryFromAst) {
        String id = getId(resultNode.getMergedField().getSingleField());
        List<NormalizedQueryField> normalizedFields = assertNotNull(normalizedQueryFromAst.getNormalizedFieldsByFieldId(id));

        for (NormalizedQueryField normalizedField : normalizedFields) {
            if (resultNode.getObjectType() == normalizedField.getObjectType() &&
                    resultNode.getFieldDefinition() == normalizedField.getFieldDefinition()) {
                return normalizedField;
            }
        }
        return Assert.assertShouldNeverHappen("Can't find normalized query field");
    }

}
