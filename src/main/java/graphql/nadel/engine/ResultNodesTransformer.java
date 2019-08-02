package graphql.nadel.engine;

import graphql.Internal;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.util.NodeAdapter;
import graphql.util.NodeMultiZipper;
import graphql.util.NodeZipper;
import graphql.util.TraversalControl;
import graphql.util.Traverser;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitor;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static graphql.Assert.assertNotNull;
import static graphql.execution.nextgen.result.ResultNodeAdapter.RESULT_NODE_ADAPTER;

@Internal
public class ResultNodesTransformer {


    public ExecutionResultNode transform(ExecutionResultNode root, TraverserVisitor<ExecutionResultNode> visitor) {
        return transform(root, visitor, Collections.emptyMap());
    }

    public ExecutionResultNode transform(ExecutionResultNode root, TraverserVisitor<ExecutionResultNode> traverserVisitor, Map<Class<?>, Object> rootVars) {
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

        List<NodeZipper<ExecutionResultNode>> zippers = new LinkedList<>();
        Traverser<ExecutionResultNode> traverser = Traverser.depthFirst(ExecutionResultNode::getChildren, zippers, null);
        traverser.noCycleDetection();
        traverser.rootVars(rootVars);
        traverser.traverse(root, nodeTraverserVisitor);

        NodeMultiZipper<ExecutionResultNode> multiZipper = NodeMultiZipper.newNodeMultiZipperTrusted(root, zippers, RESULT_NODE_ADAPTER);
        return multiZipper.toRootNode();

    }
}

