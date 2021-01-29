package graphql.nadel.engine;

import graphql.Assert;
import graphql.Internal;
import graphql.execution.ExecutionPath;
import graphql.execution.ExecutionStepInfo;
import graphql.nadel.Operation;
import graphql.nadel.dsl.NodeId;
import graphql.nadel.result.ExecutionResultNode;
import graphql.schema.GraphQLSchema;
import graphql.util.Breadcrumb;
import graphql.util.NodeMultiZipper;
import graphql.util.NodeZipper;
import graphql.util.TraversalControl;
import graphql.util.Traverser;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static graphql.execution.ExecutionStepInfo.newExecutionStepInfo;
import static graphql.nadel.result.ObjectExecutionResultNode.newObjectExecutionResultNode;
import static graphql.nadel.result.ResultNodeAdapter.RESULT_NODE_ADAPTER;
import static graphql.util.FpKit.groupingBy;
import static graphql.util.FpKit.mapEntries;

@Internal
public class StrategyUtil {

    public static List<NodeMultiZipper<ExecutionResultNode>> groupNodesIntoBatchesByField(Collection<NodeZipper<ExecutionResultNode>> nodes, ExecutionResultNode root) {
        Map<List<String>, List<NodeZipper<ExecutionResultNode>>> zipperByField = groupingBy(nodes,
                (executionResultZipper -> executionResultZipper.getCurNode().getFieldIds()));
        return mapEntries(zipperByField, (key, value) -> new NodeMultiZipper<>(root, value, RESULT_NODE_ADAPTER));
    }


    public static Set<NodeZipper<ExecutionResultNode>> getHydrationInputNodes(ExecutionResultNode roots, Set<ExecutionPath> hydrationInputPaths) {
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

        Traverser<ExecutionResultNode> traverser = Traverser.depthFirst(node -> {
//                List<ExecutionResultNode> children = new ArrayList<>();
//                for (ExecutionResultNode child : node.getChildren()) {
//                    if (child instanceof HydrationInputNode) {
                        return node.getChildren();
//                    }
                    // Problem is that we can't set the index for the breadcrumbs leading to the hydration Input Node
                    // i.e tree = /foo -> /foo/bar -> /foo/bar/name, /foo/bar/nestedBar
                    // -> /foo/bar/nestedBar/name, /foo/bar/nestedBar/hydrationNode
                    // here nestedBar is the second child of the /bar but this method expects it to return a list of 2 children including name/nestedBar rather than just
                    // traversing down nestedBar. This screws up in traverserState.java because nodeLocation is based on the index of the node in the children list
//                    if (hydrationInputPaths.contains(child.getExecutionPath())) {
//                        children.add(child);
//                    }
//                }
//                return children;
        });

        traverser.traverse(roots, new TraverserVisitorStub<ExecutionResultNode>() {
            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                if (context.thisNode() instanceof HydrationInputNode) {
                    result.add(new NodeZipper<>(context.thisNode(), context.getBreadcrumbs(), RESULT_NODE_ADAPTER));
                }

                // This approach is a bit worse than modifying the DFS algorithm however it works
                // It aborts traversal as soon as it gets to a node that isn't along the hydrationInputPaths
                if (hydrationInputPaths.contains(context.thisNode().getExecutionPath())) {
                    return TraversalControl.CONTINUE;
                } else {
                    return TraversalControl.ABORT;
                }
            }

        });
        return result;
    }

    public static ExecutionStepInfo createRootExecutionStepInfo(GraphQLSchema graphQLSchema, Operation operation) {
        ExecutionStepInfo executionInfo = newExecutionStepInfo().type(operation.getRootType(graphQLSchema)).path(ExecutionPath.rootPath()).build();
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
                .executionPath(from.getExecutionPath())
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
                .executionPath(from.getPath())
                .build();
    }


}
