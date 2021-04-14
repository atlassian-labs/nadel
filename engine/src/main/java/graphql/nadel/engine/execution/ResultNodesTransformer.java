package graphql.nadel.engine.execution;

import graphql.Internal;
import graphql.nadel.engine.result.ExecutionResultNode;
import graphql.util.TraverserVisitor;
import graphql.util.TreeTransformer;

import static graphql.Assert.assertNotNull;
import static graphql.nadel.engine.result.ResultNodeAdapter.RESULT_NODE_ADAPTER;

@Internal
public class ResultNodesTransformer {

    public ExecutionResultNode transform(ExecutionResultNode root, TraverserVisitor<ExecutionResultNode> traverserVisitor) {
        assertNotNull(root);

        TreeTransformer<ExecutionResultNode> treeTransformer = new TreeTransformer<>(RESULT_NODE_ADAPTER);
        return treeTransformer.transform(root, traverserVisitor);
    }
}

