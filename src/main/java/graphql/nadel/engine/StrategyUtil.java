package graphql.nadel.engine;

import graphql.Assert;
import graphql.Internal;
import graphql.execution.ExecutionPath;
import graphql.execution.ExecutionStepInfo;
import graphql.nadel.Operation;
import graphql.nadel.dsl.NodeId;
import graphql.nadel.result.ExecutionResultNode;
import graphql.nadel.result.LeafExecutionResultNode;
import graphql.nadel.result.ListExecutionResultNode;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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


    public static Set<NodeZipper<ExecutionResultNode>> getHydrationInputNodes(ExecutionResultNode roots) {
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
        Set<ExecutionResultNode> parents = new HashSet<>();

        traverser.traverse(roots, new TraverserVisitorStub<ExecutionResultNode>() {
            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                if (context.thisNode() instanceof HydrationInputNode) {
                    HydrationInputNode hydrationNode = (HydrationInputNode) context.thisNode();

                    if (parents.contains(context.getParentNode())) {
                        Object value;
                        //we want to change the hydrationInputNodes value to the parents completed value so that we can have multiple source objects
                        if (context.getParentNode() instanceof ListExecutionResultNode) {
                            value = context.getParentContext().getParentNode().getCompletedValue();
                        } else {
                            value = context.getParentNode().getCompletedValue();
                        }
                        hydrationNode = (HydrationInputNode) context.thisNode().transform(builder -> builder.completedValue(value));
                    }

                    NodeZipper<ExecutionResultNode> nodeZipper = new NodeZipper<>(hydrationNode, context.getBreadcrumbs(), RESULT_NODE_ADAPTER);
                    result.add(nodeZipper);
                } else {
                    parents.add(context.thisNode());
//                    if (context.thisNode() instanceof LeafExecutionResultNode && context.thisNode().getFieldName().equals("extra_source_arg_")) {
//                        context.deleteNode();
//                    }
                }
                return TraversalControl.CONTINUE;
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
