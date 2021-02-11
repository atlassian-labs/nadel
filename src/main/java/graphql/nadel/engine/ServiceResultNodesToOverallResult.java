package graphql.nadel.engine;

import graphql.GraphQLError;
import graphql.Internal;
import graphql.execution.ExecutionId;
import graphql.execution.MergedField;
import graphql.execution.ResultPath;
import graphql.language.AbstractNode;
import graphql.nadel.Tuples;
import graphql.nadel.TuplesTwo;
import graphql.nadel.dsl.NodeId;
import graphql.nadel.engine.transformation.FieldRenameTransformation;
import graphql.nadel.engine.transformation.FieldTransformation;
import graphql.nadel.engine.transformation.HydrationTransformation;
import graphql.nadel.engine.transformation.TransformationMetadata;
import graphql.nadel.engine.transformation.TransformationMetadata.NormalizedFieldAndError;
import graphql.nadel.engine.transformation.UnapplyResult;
import graphql.nadel.normalized.NormalizedQueryField;
import graphql.nadel.normalized.NormalizedQueryFromAst;
import graphql.nadel.result.ExecutionResultNode;
import graphql.nadel.result.LeafExecutionResultNode;
import graphql.nadel.result.ListExecutionResultNode;
import graphql.nadel.result.ObjectExecutionResultNode;
import graphql.nadel.result.ResultCounter;
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
import java.util.stream.Collectors;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertShouldNeverHappen;
import static graphql.Assert.assertTrue;
import static graphql.nadel.engine.ExecutionResultNodeMapper.checkForTypeRename;
import static graphql.nadel.engine.StrategyUtil.changeFieldIsInResultNode;
import static graphql.nadel.util.FpKit.getSingleMapValue;
import static graphql.util.FpKit.groupingBy;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

@Internal
public class ServiceResultNodesToOverallResult {

    ExecutionResultNodeMapper executionResultNodeMapper = new ExecutionResultNodeMapper();

    ResolvedValueMapper resolvedValueMapper = new ResolvedValueMapper();

    ResultNodesTransformer resultNodesTransformer = new ResultNodesTransformer();

    private static final Logger log = LoggerFactory.getLogger(ServiceResultNodesToOverallResult.class);

    @SuppressWarnings("UnnecessaryLocalVariable")
    public ExecutionResultNode convert(ExecutionId executionId,
                                       ExecutionResultNode resultNode,
                                       GraphQLSchema overallSchema,
                                       ExecutionResultNode correctRootNode,
                                       Map<String, FieldTransformation> fieldIdToTransformation,
                                       Map<String, String> typeRenameMappings,
                                       NadelContext nadelContext,
                                       TransformationMetadata transformationMetadata) {
        return convertImpl(executionId, resultNode, null, overallSchema, correctRootNode, false, false, fieldIdToTransformation, typeRenameMappings, false, nadelContext, transformationMetadata);
    }

    public ExecutionResultNode convertChildren(ExecutionId executionId,
                                               ExecutionResultNode root,
                                               NormalizedQueryField normalizedRootField,
                                               GraphQLSchema overallSchema,
                                               ExecutionResultNode correctRootNode,
                                               boolean isHydrationTransformation,
                                               boolean batched,
                                               Map<String, FieldTransformation> fieldIdToTransformation,
                                               Map<String, String> typeRenameMappings,
                                               NadelContext nadelContext,
                                               TransformationMetadata transformationMetadata) {
        return convertImpl(executionId, root, normalizedRootField, overallSchema, correctRootNode, isHydrationTransformation, batched, fieldIdToTransformation, typeRenameMappings, true, nadelContext, transformationMetadata);
    }

    private ExecutionResultNode convertImpl(ExecutionId executionId,
                                            ExecutionResultNode root,
                                            NormalizedQueryField normalizedRootField,
                                            GraphQLSchema overallSchema,
                                            ExecutionResultNode correctRootNode,
                                            boolean isHydrationTransformation,
                                            boolean batched,
                                            Map<String, FieldTransformation> fieldIdToTransformation,
                                            Map<String, String> typeRenameMappings,
                                            boolean onlyChildren,
                                            NadelContext nadelContext,
                                            TransformationMetadata transformationMetadata) {
        ResultCounter resultCounter = new ResultCounter();

        HandleResult handleResult = convertSingleNode(root,
                null/*not for root*/,
                executionId,
                root,
                normalizedRootField,
                overallSchema,
                isHydrationTransformation,
                batched,
                fieldIdToTransformation,
                typeRenameMappings,
                onlyChildren,
                nadelContext,
                transformationMetadata,
                resultCounter);
        assertNotNull(handleResult, () -> "can't delete root");
        assertTrue(handleResult.siblings.isEmpty(), () -> "can't add siblings to root");

        ExecutionResultNode changedNode = handleResult.changedNode;
        List<ExecutionResultNode> newChildren = new ArrayList<>();
        for (ExecutionResultNode child : changedNode.getChildren()) {
            // pass in the correct root node as parent, not root
            HandleResult handleResultChild = convertRecursively(child,
                    correctRootNode,
                    executionId,
                    root,
                    normalizedRootField,
                    overallSchema,
                    isHydrationTransformation,
                    batched,
                    fieldIdToTransformation,
                    typeRenameMappings,
                    onlyChildren,
                    nadelContext,
                    transformationMetadata,
                    resultCounter);
            if (handleResultChild == null) {
                continue;
            }
            newChildren.add(handleResultChild.changedNode);
            newChildren.addAll(handleResultChild.siblings);
        }

        return changedNode.transform(
                builder -> builder
                        .children(newChildren)
                        .totalNodeCount(resultCounter.getNodeCount())
                        .totalFieldRenameCount(resultCounter.getFieldRenameCount())
                        .totalTypeRenameCount(resultCounter.getTypeRenameCount())
        );
    }

    private HandleResult convertRecursively(ExecutionResultNode node,
                                            ExecutionResultNode parentNode,
                                            ExecutionId executionId,
                                            ExecutionResultNode root,
                                            NormalizedQueryField normalizedRootField,
                                            GraphQLSchema overallSchema,
                                            boolean isHydrationTransformation,
                                            boolean batched,
                                            Map<String, FieldTransformation> fieldIdToTransformation,
                                            Map<String, String> typeRenameMappings,
                                            boolean onlyChildren,
                                            NadelContext nadelContext,
                                            TransformationMetadata transformationMetadata,
                                            ResultCounter resultCounter) {
        HandleResult handleResult = convertSingleNode(node, parentNode, executionId, root, normalizedRootField, overallSchema, isHydrationTransformation, batched, fieldIdToTransformation, typeRenameMappings, onlyChildren, nadelContext, transformationMetadata, resultCounter);
        if (handleResult == null) {
            return null;
        }
        if (handleResult.traversalControl == TraversalControl.ABORT) {
            return handleResult;
        }
        ExecutionResultNode changedNode = handleResult.changedNode;
        List<ExecutionResultNode> newChildren = new ArrayList<>();
        for (ExecutionResultNode child : changedNode.getChildren()) {
            HandleResult handleResultChild = convertRecursively(child, changedNode, executionId, root, normalizedRootField, overallSchema, isHydrationTransformation, batched, fieldIdToTransformation, typeRenameMappings, onlyChildren, nadelContext, transformationMetadata, resultCounter);
            if (handleResultChild == null) {
                continue;
            }
            newChildren.add(handleResultChild.changedNode);
            // additional siblings are not descended, just added
            newChildren.addAll(handleResultChild.siblings);
        }
        handleResult.changedNode = changedNode.withNewChildren(newChildren);
        return handleResult;
    }

    private HandleResult convertSingleNode(ExecutionResultNode node,
                                           ExecutionResultNode parentNode,
                                           ExecutionId executionId,
                                           ExecutionResultNode root,
                                           NormalizedQueryField normalizedRootField,
                                           GraphQLSchema overallSchema,
                                           boolean isHydrationTransformation,
                                           boolean batched,
                                           Map<String, FieldTransformation> fieldIdTransformation,
                                           Map<String, String> typeRenameMappings,
                                           boolean onlyChildren,
                                           NadelContext nadelContext,
                                           TransformationMetadata transformationMetadata,
                                           ResultCounter resultCounter) {
        resultCounter.incrementNodeCount();

        if (onlyChildren && node == root) {
            // Could be a possible type rename if this is hydrated
            if (normalizedRootField != null) {
                checkForTypeRename(normalizedRootField.getFieldDefinition(), node.getFieldDefinition(), typeRenameMappings, resultCounter, 0);
            }
            if (root instanceof ObjectExecutionResultNode) {
                ExecutionResultNode executionResultNode = addDeletedChildren((ObjectExecutionResultNode) node, normalizedRootField, nadelContext, transformationMetadata);
                return HandleResult.simple(executionResultNode);
            } else {
                return HandleResult.simple(node);
            }
        }

        if (node instanceof RootExecutionResultNode) {
            ExecutionResultNode convertedNode = mapRootResultNode((RootExecutionResultNode) node);
            return HandleResult.simple(convertedNode);
        }
        if (node instanceof LeafExecutionResultNode) {
            LeafExecutionResultNode leaf = (LeafExecutionResultNode) node;
            if (ArtificialFieldUtils.isArtificialField(nadelContext, leaf.getAlias())) {
                resultCounter.decrementNodeCount();
                return null;
            }
        }

        TuplesTwo<Set<FieldTransformation>, List<String>> transformationsAndNotTransformedFields =
                getTransformationsAndNotTransformedFields(node, fieldIdTransformation, transformationMetadata);

        List<FieldTransformation> transformations = new ArrayList<>(transformationsAndNotTransformedFields.getT1());

        UnapplyEnvironment unapplyEnvironment = new UnapplyEnvironment(
                parentNode,
                isHydrationTransformation,
                batched,
                typeRenameMappings,
                overallSchema
        );
        HandleResult result;
        if (transformations.size() == 0) {
            result = HandleResult.simple(mapNode(node, unapplyEnvironment, resultCounter));
        } else {
            result = unapplyTransformations(executionId, node, transformations, unapplyEnvironment, fieldIdTransformation, nadelContext, transformationMetadata, resultCounter);
        }

        if (result.changedNode instanceof ObjectExecutionResultNode && !(parentNode instanceof HydrationInputNode)) {
            result.changedNode = addDeletedChildren((ObjectExecutionResultNode) result.changedNode, null, nadelContext, transformationMetadata);
        }
        return result;
    }

    private ExecutionResultNode addDeletedChildren(ObjectExecutionResultNode resultNode,
                                                   NormalizedQueryField normalizedQueryField,
                                                   NadelContext nadelContext,
                                                   TransformationMetadata transformationMetadata
    ) {
        if (normalizedQueryField == null) {
            normalizedQueryField = getNormalizedQueryFieldForResultNode(resultNode, nadelContext.getNormalizedOverallQuery());
        }
        List<NormalizedFieldAndError> removedFields = transformationMetadata.getRemovedFieldsForParent(normalizedQueryField);
        for (NormalizedFieldAndError normalizedFieldAndError : removedFields) {
            MergedField mergedField = nadelContext.getNormalizedOverallQuery().getMergedFieldByNormalizedFields().get(normalizedFieldAndError.getNormalizedField());
            LeafExecutionResultNode newChild = createRemovedFieldResult(resultNode, mergedField, normalizedFieldAndError.getNormalizedField(), normalizedFieldAndError.getError());
            resultNode = resultNode.transform(b -> b.addChild(newChild));
        }
        return resultNode;
    }

    private LeafExecutionResultNode createRemovedFieldResult(ExecutionResultNode parent,
                                                             MergedField mergedField,
                                                             NormalizedQueryField normalizedQueryField,
                                                             GraphQLError error) {
        ResultPath parentPath = parent.getResultPath();
        ResultPath executionPath = parentPath.segment(normalizedQueryField.getResultKey());

        LeafExecutionResultNode removedNode = LeafExecutionResultNode.newLeafExecutionResultNode()
                .executionPath(executionPath)
                .alias(mergedField.getSingleField().getAlias())
                .fieldIds(NodeId.getIds(mergedField))
                .objectType(normalizedQueryField.getObjectType())
                .fieldDefinition(normalizedQueryField.getFieldDefinition())
                .completedValue(null)
                .errors(singletonList(error))
                .build();
        return removedNode;
    }

    private HandleResult unapplyTransformations(ExecutionId executionId,
                                                ExecutionResultNode node,
                                                List<FieldTransformation> transformations,
                                                UnapplyEnvironment unapplyEnvironment,
                                                Map<String, FieldTransformation> fieldIdToTransformation,
                                                NadelContext nadelContext,
                                                TransformationMetadata transformationMetadata,
                                                ResultCounter resultCounter) {

        HandleResult handleResult;
        FieldTransformation transformation = transformations.get(0);

        if (transformation instanceof HydrationTransformation) {
            handleResult = unapplyHydration(node, transformations, unapplyEnvironment, fieldIdToTransformation, transformation, transformationMetadata, resultCounter);
        } else if (transformation instanceof FieldRenameTransformation) {
            handleResult = unapplyFieldRename(executionId, node, transformations, unapplyEnvironment, fieldIdToTransformation, nadelContext, transformationMetadata, resultCounter);
        } else {
            return assertShouldNeverHappen("Unexpected transformation type " + transformation);
        }
        return handleResult;
    }

    private HandleResult unapplyFieldRename(ExecutionId executionId,
                                            ExecutionResultNode node,
                                            List<FieldTransformation> transformations,
                                            UnapplyEnvironment unapplyEnvironment,
                                            Map<String, FieldTransformation> fieldIdToTransformation,
                                            NadelContext nadelContext,
                                            TransformationMetadata transformationMetadata,
                                            ResultCounter resultCounter) {
        Map<AbstractNode, ? extends List<FieldTransformation>> transformationByDefinition = groupingBy(transformations, FieldTransformation::getDefinition);

        TuplesTwo<ExecutionResultNode, Map<AbstractNode, ExecutionResultNode>> splittedNodes = splitTreeByTransformationDefinition(node, fieldIdToTransformation, transformationMetadata);
        ExecutionResultNode notTransformedTree = splittedNodes.getT1();
        Map<AbstractNode, ExecutionResultNode> nodesWithTransformedFields = splittedNodes.getT2();

        List<UnapplyResult> unapplyResults = new ArrayList<>();
        for (AbstractNode definition : nodesWithTransformedFields.keySet()) {
            List<FieldTransformation> transformationsForDefinition = transformationByDefinition.get(definition);
            UnapplyResult unapplyResult = transformationsForDefinition.get(0).unapplyResultNode(nodesWithTransformedFields.get(definition), transformationsForDefinition, unapplyEnvironment);

            // typeDecrementAmount = 0 because for a field rename it's children will not know about the underlying type.
            checkForTypeRename(unapplyResult.getNode().getFieldDefinition(), node.getFieldDefinition(), unapplyEnvironment.typeRenameMappings, resultCounter, 0);

            unapplyResults.add(unapplyResult);
        }
        resultCounter.incrementFieldRenameCount(unapplyResults.size());
        HandleResult handleResult = HandleResult.newHandleResultWithSiblings();
        boolean first = true;
        // the not transformed part should simply continue to be converted
        if (notTransformedTree != null) {
            ExecutionResultNode mappedNode = mapNode(node, unapplyEnvironment, resultCounter);
            mappedNode = convertChildren(executionId,
                    mappedNode,
                    null,
                    unapplyEnvironment.overallSchema,
                    unapplyEnvironment.parentNode,
                    unapplyEnvironment.isHydrationTransformation,
                    unapplyEnvironment.batched,
                    fieldIdToTransformation,
                    unapplyEnvironment.typeRenameMappings,
                    nadelContext,
                    transformationMetadata);
            handleResult.changedNode = mappedNode;
            resultCounter.incrementFieldRenameCount(mappedNode.getTotalFieldRenameCount());
            resultCounter.incrementTypeRenameCount(mappedNode.getTotalTypeRenameCount());
            first = false;
        }

        // each unapply result is either continued to processed
        for (UnapplyResult unapplyResult : unapplyResults) {
            ExecutionResultNode transformedResult;
            if (unapplyResult.getTraversalControl() != TraversalControl.CONTINUE) {
                transformedResult = unapplyResult.getNode();
            } else {
                ExecutionResultNode unapplyResultNode = unapplyResult.getNode();
                transformedResult = convertChildren(executionId,
                        unapplyResultNode,
                        null,
                        unapplyEnvironment.overallSchema,
                        unapplyResultNode,
                        unapplyEnvironment.isHydrationTransformation,
                        unapplyEnvironment.batched,
                        fieldIdToTransformation,
                        unapplyEnvironment.typeRenameMappings,
                        nadelContext,
                        transformationMetadata);
            }
            resultCounter.incrementFieldRenameCount(transformedResult.getTotalFieldRenameCount());
            resultCounter.incrementTypeRenameCount(transformedResult.getTotalTypeRenameCount());
            if (first) {
                handleResult.changedNode = transformedResult;
                first = false;
            } else {
                handleResult.siblings.add(transformedResult);
            }
        }
        handleResult.traversalControl = TraversalControl.ABORT;
        return handleResult;
    }

    private HandleResult unapplyHydration(ExecutionResultNode node,
                                          List<FieldTransformation> transformations,
                                          UnapplyEnvironment unapplyEnvironment,
                                          Map<String, FieldTransformation> fieldIdToTransformation,
                                          FieldTransformation transformation,
                                          TransformationMetadata transformationMetadata,
                                          ResultCounter resultCounter
    ) {
        HandleResult handleResult = HandleResult.newHandleResultWithSiblings();

        TuplesTwo<ExecutionResultNode, Map<AbstractNode, ExecutionResultNode>> splittedNodes = splitTreeByTransformationDefinition(node, fieldIdToTransformation, transformationMetadata);
        ExecutionResultNode withoutTransformedFields = splittedNodes.getT1();
        assertTrue(splittedNodes.getT2().size() == 1, () -> "only one split tree expected atm");
        ExecutionResultNode nodesWithTransformedFields = getSingleMapValue(splittedNodes.getT2());

        UnapplyResult unapplyResult = transformation.unapplyResultNode(nodesWithTransformedFields, transformations, unapplyEnvironment);

        // For every list node, it's children will also have a renamed type so the type rename count is decremented based on
        // the size of it's children.
        // //E.g. /foo , /foo[0], /foo[1], /foo[2] => type rename count becomes -2, -1, 0, 1
        int typeDecrementValue = unapplyResult.getNode() instanceof ListExecutionResultNode ? -unapplyResult.getNode().getChildren().size() : -1;
        checkForTypeRename(unapplyResult.getNode().getFieldDefinition(), node.getFieldDefinition(), unapplyEnvironment.typeRenameMappings, resultCounter, typeDecrementValue);

        if (withoutTransformedFields != null) {
            handleResult.changedNode = mapNode(withoutTransformedFields, unapplyEnvironment, resultCounter);
            handleResult.siblings.add(unapplyResult.getNode());
            handleResult.traversalControl = TraversalControl.CONTINUE;
            return handleResult;
        } else {
            handleResult.changedNode = unapplyResult.getNode();
            handleResult.traversalControl = unapplyResult.getTraversalControl();
            return handleResult;
        }
    }

    private TuplesTwo<ExecutionResultNode, Map<AbstractNode, ExecutionResultNode>> splitTreeByTransformationDefinition(
            ExecutionResultNode executionResultNode,
            Map<String, FieldTransformation> fieldIdToTransformation,
            TransformationMetadata transformationMetadata) {
        if (executionResultNode instanceof RootExecutionResultNode) {
            return Tuples.of(executionResultNode, emptyMap());
        }

        Map<AbstractNode, Set<String>> transformationIdsByTransformationDefinition = new LinkedHashMap<>();
        List<String> fieldIds = executionResultNode.getFieldIds();
        for (String fieldId : fieldIds) {
            List<String> transformationIds = FieldMetadataUtil.getRootOfTransformationIds(fieldId, transformationMetadata.getMetadataByFieldId());
            for (String transformationId : transformationIds) {
                FieldTransformation fieldTransformation = assertNotNull(fieldIdToTransformation.get(transformationId));
                AbstractNode definition = fieldTransformation.getDefinition();
                transformationIdsByTransformationDefinition.putIfAbsent(definition, new LinkedHashSet<>());
                transformationIdsByTransformationDefinition.get(definition).add(transformationId);
            }
        }
        Map<AbstractNode, ExecutionResultNode> treesByDefinition = new LinkedHashMap<>();
        for (AbstractNode definition : transformationIdsByTransformationDefinition.keySet()) {
            Set<String> transformationIds = transformationIdsByTransformationDefinition.get(definition);
            treesByDefinition.put(definition, nodesWithTransformationIds(executionResultNode, transformationIds, transformationMetadata));
        }
        ExecutionResultNode treeWithout = nodesWithTransformationIds(executionResultNode, null, transformationMetadata);
        return Tuples.of(treeWithout, treesByDefinition);
    }

    private ExecutionResultNode nodesWithTransformationIds(ExecutionResultNode
                                                                   executionResultNode, Set<String> transformationIds, TransformationMetadata transformationMetadata) {
        return resultNodesTransformer.transform(executionResultNode, new TraverserVisitorStub<ExecutionResultNode>() {

            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                ExecutionResultNode node = context.thisNode();
                List<String> fieldIdsWithId;
                if (transformationIds == null) {
                    fieldIdsWithId = getFieldIdsWithoutTransformationId(node, transformationMetadata);
                } else {
                    fieldIdsWithId = getFieldIdsWithTransformationIds(node, transformationIds, transformationMetadata);
                }

                if (fieldIdsWithId.size() == 0) {
                    return TreeTransformerUtil.deleteNode(context);
                }
                ExecutionResultNode changedNode = changeFieldIsInResultNode(node, fieldIdsWithId);
                return TreeTransformerUtil.changeNode(context, changedNode);
            }
        });
    }

    private List<String> getFieldIdsWithoutTransformationId(ExecutionResultNode node, TransformationMetadata transformationMetadata) {
        return node.getFieldIds().stream().filter(fieldId -> FieldMetadataUtil.getTransformationIds(fieldId, transformationMetadata.getMetadataByFieldId()).size() == 0).collect(Collectors.toList());
    }

    private List<String> getFieldIdsWithTransformationIds(ExecutionResultNode
                                                                  node, Set<String> transformationIds, TransformationMetadata transformationMetadata) {
        return node.getFieldIds().stream().filter(fieldId -> {
            List<String> transformationIdsForField = FieldMetadataUtil.getTransformationIds(fieldId, transformationMetadata.getMetadataByFieldId());
            return transformationIdsForField.containsAll(transformationIds);
        }).collect(Collectors.toList());
    }

    private ExecutionResultNode mapNode(ExecutionResultNode node, UnapplyEnvironment environment, ResultCounter resultCounter) {
        ExecutionResultNode mappedNode = executionResultNodeMapper.mapERNFromUnderlyingToOverall(node, environment, resultCounter);
        mappedNode = resolvedValueMapper.mapCompletedValue(mappedNode, environment);
        return mappedNode;
    }

    private TuplesTwo<Set<FieldTransformation>, List<String>> getTransformationsAndNotTransformedFields(
            ExecutionResultNode node,
            Map<String, FieldTransformation> fieldIdToTransformation,
            TransformationMetadata transformationMetadata
    ) {
        Set<FieldTransformation> transformations = new LinkedHashSet<>();
        List<String> notTransformedFields = new ArrayList<>();
        for (String fieldId : node.getFieldIds()) {

            if (node.getResultPath().isListSegment()) {
                notTransformedFields.add(fieldId);
                continue;
            }

            List<String> rootTransformationIds = FieldMetadataUtil.getRootOfTransformationIds(fieldId, transformationMetadata.getMetadataByFieldId());
            if (rootTransformationIds.size() == 0) {
                notTransformedFields.add(fieldId);
                continue;
            }
            for (String transformationId : rootTransformationIds) {
                FieldTransformation fieldTransformation = fieldIdToTransformation.get(transformationId);
                transformations.add(fieldTransformation);
            }
        }
        return Tuples.of(transformations, notTransformedFields);
    }

    private RootExecutionResultNode mapRootResultNode(RootExecutionResultNode resultNode) {
        return RootExecutionResultNode.newRootExecutionResultNode()
                .children(resultNode.getChildren())
                .errors(resultNode.getErrors())
                .extensions(resultNode.getExtensions())
                .elapsedTime(resultNode.getElapsedTime())
                .build();
    }

    private NormalizedQueryField getNormalizedQueryFieldForResultNode(ObjectExecutionResultNode resultNode,
                                                                      NormalizedQueryFromAst normalizedQueryFromAst) {
        String id = resultNode.getFieldIds().get(0);
        List<NormalizedQueryField> normalizedFields = assertNotNull(normalizedQueryFromAst.getNormalizedFieldsByFieldId(id));

        for (NormalizedQueryField normalizedField : normalizedFields) {
            if (resultNode.getObjectType() == normalizedField.getObjectType() &&
                    resultNode.getFieldDefinition() == normalizedField.getFieldDefinition()) {
                return normalizedField;
            }
        }
        return assertShouldNeverHappen("Can't find normalized query field");
    }

    public static class HandleResult {
        ExecutionResultNode changedNode;
        List<ExecutionResultNode> siblings = emptyList();
        TraversalControl traversalControl = TraversalControl.CONTINUE;

        public HandleResult() {

        }

        public static HandleResult newHandleResultWithSiblings() {
            HandleResult handleResult = new HandleResult();
            handleResult.siblings = new ArrayList<>();
            return handleResult;
        }

        public HandleResult(ExecutionResultNode changedNode, List<ExecutionResultNode> siblings, TraversalControl traversalControl) {
            this.changedNode = changedNode;
            this.siblings = siblings;
            this.traversalControl = traversalControl;
        }

        public static HandleResult simple(ExecutionResultNode executionResultNode) {
            HandleResult handleResult = new HandleResult(executionResultNode, emptyList(), TraversalControl.CONTINUE);
            return handleResult;
        }
    }
}
