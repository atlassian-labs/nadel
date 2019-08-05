package graphql.nadel.engine;

import graphql.Internal;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.util.NodeAdapter;
import graphql.util.NodeMultiZipper;
import graphql.util.NodeZipper;
import graphql.util.ParallelTraverser;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static graphql.Assert.assertNotNull;
import static graphql.execution.nextgen.result.ResultNodeAdapter.RESULT_NODE_ADAPTER;

@Internal
public class ResultNodesTransformer {


    public ExecutionResultNode transformParallel(ExecutionResultNode root, TraverserVisitor<ExecutionResultNode> traverserVisitor) {
        return transformParallel(root, traverserVisitor, Collections.emptyMap());
    }

    public ExecutionResultNode transformParallel(ExecutionResultNode root, TraverserVisitor<ExecutionResultNode> traverserVisitor, Map<Class<?>, Object> rootVars) {
        assertNotNull(root);

        TraverserVisitor<ExecutionResultNode> nodeTraverserVisitor = new TraverserVisitor<ExecutionResultNode>() {

            @Override
            public TraversalControl enter(TraverserContext<ExecutionResultNode> context) {
                NodeZipper<ExecutionResultNode> nodeZipper = new NodeZipper<>(context.thisNode(), context.getBreadcrumbs(), RESULT_NODE_ADAPTER);
                context.setVar(NodeZipper.class, nodeZipper);
                context.setVar(NodeAdapter.class, RESULT_NODE_ADAPTER);
                return traverserVisitor.enter(context);
            }

            @Override
            public TraversalControl leave(TraverserContext<ExecutionResultNode> context) {
                return traverserVisitor.leave(context);
            }
        };

        Queue<NodeZipper<ExecutionResultNode>> zippers = new ConcurrentLinkedQueue<>();
        ParallelTraverser<ExecutionResultNode> traverser = ParallelTraverser.parallelTraverser(ExecutionResultNode::getChildren, zippers);
        traverser.rootVars(rootVars);
        traverser.traverse(root, nodeTraverserVisitor);

        NodeMultiZipper<ExecutionResultNode> multiZipper = NodeMultiZipper.newNodeMultiZipperTrusted(root, new ArrayList<>(zippers), RESULT_NODE_ADAPTER);
        return multiZipper.toRootNode();

    }
}

