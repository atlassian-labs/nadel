package graphql.nadel.engine.execution;

import graphql.Assert;
import graphql.GraphQLError;
import graphql.Internal;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.nadel.OperationKind;
import graphql.nadel.dsl.NodeId;
import graphql.nadel.engine.result.ExecutionResultNode;
import graphql.nadel.engine.result.ObjectExecutionResultNode;
import graphql.nadel.engine.result.RootExecutionResultNode;
import graphql.nadel.util.FpKit;
import graphql.schema.GraphQLSchema;
import graphql.util.Breadcrumb;
import graphql.util.NodeMultiZipper;
import graphql.util.NodeZipper;
import graphql.util.TraversalControl;
import graphql.util.Traverser;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static graphql.execution.ExecutionStepInfo.newExecutionStepInfo;
import static graphql.nadel.engine.result.ObjectExecutionResultNode.newObjectExecutionResultNode;
import static graphql.nadel.engine.result.ResultNodeAdapter.RESULT_NODE_ADAPTER;
import static graphql.nadel.engine.result.RootExecutionResultNode.newRootExecutionResultNode;
import static graphql.nadel.util.FpKit.map;
import static graphql.util.FpKit.groupingBy;
import static graphql.util.FpKit.mapEntries;

@Internal
public class StrategyUtil {

    public static List<NodeMultiZipper<ExecutionResultNode>> groupNodesIntoBatchesByField(Collection<NodeZipper<ExecutionResultNode>> nodes, ExecutionResultNode root) {
        Map<List<String>, ? extends List<NodeZipper<ExecutionResultNode>>> zipperByField = groupingBy(nodes,
                (executionResultZipper -> executionResultZipper.getCurNode().getFieldIds()));
        return mapEntries(zipperByField, (key, value) -> new NodeMultiZipper<>(root, value, RESULT_NODE_ADAPTER));
    }


    public static Set<NodeZipper<ExecutionResultNode>> getHydrationInputNodes(ExecutionResultNode roots, Set<ResultPath> hydrationInputPaths) {
        Comparator<NodeZipper<ExecutionResultNode>> comparator = (node1, node2) -> {
            if (node1 == node2) {
                return 0;
            }
            List<Breadcrumb<ExecutionResultNode>> breadcrumbs1 = node1.getBreadcrumbs();
            List<Breadcrumb<ExecutionResultNode>> breadcrumbs2 = node2.getBreadcrumbs();
            if (breadcrumbs1.size() != breadcrumbs2.size()) {
                return Integer.compare(breadcrumbs1.size(), breadcrumbs2.size());
            }
            for (int i = breadcrumbs1.size() - 1; i >= 0; i--) {
                int ix1 = breadcrumbs1.get(i).getLocation().getIndex();
                int ix2 = breadcrumbs2.get(i).getLocation().getIndex();
                if (ix1 != ix2) {
                    return Integer.compare(ix1, ix2);
                }
            }
            return Assert.assertShouldNeverHappen();
        };
        Set<NodeZipper<ExecutionResultNode>> result = Collections.synchronizedSet(new TreeSet<>(comparator));

        Traverser<ExecutionResultNode> traverser = Traverser.depthFirst(ExecutionResultNode::getChildren);

        traverser.traverse(roots, new TraverserVisitorStub<ExecutionResultNode>() {
            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                if (context.thisNode() instanceof HydrationInputNode) {
                    result.add(new NodeZipper<>(context.thisNode(), context.getBreadcrumbs(), RESULT_NODE_ADAPTER));
                }
                if (hydrationInputPaths.contains(context.thisNode().getResultPath())) {
                    return TraversalControl.CONTINUE;
                } else {
                    return TraversalControl.ABORT;
                }
            }

        });
        return result;
    }

    public static ExecutionStepInfo createRootExecutionStepInfo(GraphQLSchema graphQLSchema, OperationKind operationKind) {
        ExecutionStepInfo executionInfo = newExecutionStepInfo().type(operationKind.getRootType(graphQLSchema)).path(ResultPath.rootPath()).build();
        return executionInfo;
    }

    public static <T extends ExecutionResultNode> T changeFieldIsInResultNode(T executionResultNode, List<String> fieldIds) {
        return (T) executionResultNode.transform(builder -> builder.fieldIds(fieldIds));
    }

    public static <T extends ExecutionResultNode> T changeFieldIdsInResultNode(T executionResultNode, String fieldId) {
        return (T) executionResultNode.transform(builder -> builder.fieldId(fieldId));
    }

    public static <T extends ExecutionResultNode> T copyFieldInformation(ExecutionResultNode from, T to) {
        return (T) to.transform(builder -> builder
                .resultPath(from.getResultPath())
                .fieldIds(from.getFieldIds())
                .alias(from.getAlias())
                .objectType(from.getObjectType())
                .fieldDefinition(from.getFieldDefinition()));
    }

    public static ExecutionResultNode copyTypeInformation(ExecutionStepInfo from) {
        return newObjectExecutionResultNode()
                .fieldIds(NodeId.getIds(from.getField()))
                .objectType(from.getFieldContainer())
                .fieldDefinition(from.getFieldDefinition())
                .resultPath(from.getPath())
                .build();
    }


    /**
     * This will merge together a list of root result nodes into one root result node.
     *
     * If there are child nodes that resolve to the same result key then they will be combined
     *
     * @param rootNodes the nodes to merge
     *
     * @return a single root result node
     */
    public static RootExecutionResultNode mergeTrees(List<RootExecutionResultNode> rootNodes) {
        List<ExecutionResultNode> mergedChildren = new ArrayList<>();
        List<GraphQLError> errors = new ArrayList<>();
        map(rootNodes, RootExecutionResultNode::getChildren).forEach(mergedChildren::addAll);
        map(rootNodes, RootExecutionResultNode::getErrors).forEach(errors::addAll);
        Map<String, Object> extensions = new LinkedHashMap<>();
        rootNodes.forEach(node -> extensions.putAll(node.getExtensions()));

        Map<String, List<ExecutionResultNode>> resultNodesByResultKey = mergedChildren.stream().collect(Collectors.groupingBy(ExecutionResultNode::getResultKey));
        mergedChildren = new ArrayList<>();
        for (List<ExecutionResultNode> resultNodes : resultNodesByResultKey.values()) {
            ExecutionResultNode resultNode = resultNodes.get(0);
            if (resultNodes.size() > 1) {
                // we MUST have a split top level field -we need to combine it
                resultNode = combineResultNodes(resultNodes);
            }
            mergedChildren.add(resultNode);
        }

        return newRootExecutionResultNode()
                .children(mergedChildren)
                .errors(errors)
                .extensions(extensions)
                .build();
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    private static ExecutionResultNode combineResultNodes(List<ExecutionResultNode> resultNodes) {
        List<ExecutionResultNode> children = new ArrayList<>();
        Map<String, Object> completedValueMap = new LinkedHashMap<>();

        List<ExecutionResultNode> nonNullNodes =
                FpKit.filter(resultNodes, resultNode -> resultNode.getCompletedValue() != null);

        for (ExecutionResultNode resultNode : nonNullNodes) {
            Object completedValue = resultNode.getCompletedValue();

            Assert.assertTrue(resultNode instanceof ObjectExecutionResultNode, () -> String.format("We can only combine object fields not %s for result path %s", resultNode.getClass(), resultNode.getResultKey()));
            Assert.assertTrue(completedValue instanceof Map, () -> String.format("We can only combine object field values that are maps not %s for result path %s", completedValue.getClass(), resultNode.getResultKey()));
            children.addAll(resultNode.getChildren());
            Map<String, Object> childValue = (Map<String, Object>) completedValue;
            completedValueMap.putAll(childValue);
        }

        return FpKit.findOne(nonNullNodes, node -> true)
                .map(objectNode -> objectNode.transform(
                        builder -> builder.children(children).completedValue(completedValueMap)
                ))
                // all result nodes are null. So just return the first of them
                .orElse(resultNodes.get(0));
    }

}
