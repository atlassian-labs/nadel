package graphql.nadel.engine;

import graphql.Assert;
import graphql.execution.ExecutionPath;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.language.Field;
import graphql.nadel.Operation;
import graphql.nadel.result.ExecutionResultNode;
import graphql.schema.GraphQLSchema;
import graphql.util.Breadcrumb;
import graphql.util.FpKit;
import graphql.util.NodeMultiZipper;
import graphql.util.NodeZipper;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitorStub;
import graphql.util.TreeParallelTraverser;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;

import static graphql.execution.ExecutionStepInfo.newExecutionStepInfo;
import static graphql.nadel.engine.FixListNamesAdapter.FIX_NAMES_ADAPTER;
import static graphql.nadel.result.ObjectExecutionResultNode.newObjectExecutionResultNode;
import static graphql.util.FpKit.mapEntries;

public class StrategyUtil {

    public static List<NodeMultiZipper<ExecutionResultNode>> groupNodesIntoBatchesByField(Collection<NodeZipper<ExecutionResultNode>> nodes, ExecutionResultNode root) {
        Map<MergedField, List<NodeZipper<ExecutionResultNode>>> zipperByField = FpKit.groupingBy(nodes,
                (executionResultZipper -> executionResultZipper.getCurNode().getMergedField()));
        return mapEntries(zipperByField, (key, value) -> new NodeMultiZipper<>(root, value, FIX_NAMES_ADAPTER));
    }


    public static Set<NodeZipper<ExecutionResultNode>> getHydrationInputNodes(ForkJoinPool forkJoinPool, ExecutionResultNode roots) {
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

        TreeParallelTraverser<ExecutionResultNode> traverser = TreeParallelTraverser.parallelTraverser(ExecutionResultNode::getChildren, null, forkJoinPool);
        traverser.traverse(roots, new TraverserVisitorStub<ExecutionResultNode>() {
            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                if (context.thisNode() instanceof HydrationInputNode) {
                    result.add(new NodeZipper<>(context.thisNode(), context.getBreadcrumbs(), FIX_NAMES_ADAPTER));
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

    public static <T extends ExecutionResultNode> T changeFieldInResultNode(T executionResultNode, MergedField newField) {
        return (T) executionResultNode.transform(builder -> builder.field(newField));
    }

    public static <T extends ExecutionResultNode> T changeFieldInResultNode(T executionResultNode, Field newField) {
        MergedField mergedField = MergedField.newMergedField(newField).build();
        return (T) executionResultNode.transform(builder -> builder.field(mergedField));
    }

    public static <T extends ExecutionResultNode> T copyTypeInformation(ExecutionResultNode from, T to) {
        return (T) to.transform(builder -> builder
                .field(from.getField())
                .objectType(from.getObjectType())
                .fieldDefinition(from.getFieldDefinition()));
    }

    public static ExecutionResultNode copyTypeInformation(ExecutionStepInfo from) {
        return newObjectExecutionResultNode()
                .field(from.getField())
                .objectType(from.getFieldContainer())
                .fieldDefinition(from.getFieldDefinition())
                .executionPath(from.getPath())
                .build();
    }


}
