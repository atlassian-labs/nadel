package graphql.nadel.engine;

import graphql.Internal;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.util.TraverserVisitor;
import graphql.util.TreeTransformer;

import java.util.Collections;
import java.util.Map;

import static graphql.Assert.assertNotNull;
import static graphql.execution.nextgen.result.ResultNodeAdapter.RESULT_NODE_ADAPTER;

@Internal
public class ResultNodesTransformer {


    public ExecutionResultNode transform(ExecutionResultNode root, TraverserVisitor<ExecutionResultNode> visitor) {
        return transform(root, visitor, Collections.emptyMap());
    }

    public ExecutionResultNode transform(ExecutionResultNode root, TraverserVisitor<ExecutionResultNode> visitor, Map<Class<?>, Object> rootVars) {
        assertNotNull(root);

        TreeTransformer<ExecutionResultNode> treeTransformer = new TreeTransformer<>(RESULT_NODE_ADAPTER);
        return treeTransformer.transform(root, visitor, rootVars);
    }
}

