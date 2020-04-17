package graphql.nadel.engine;

import graphql.Internal;
import graphql.nadel.result.ExecutionResultNode;
import graphql.util.TraverserVisitor;
import graphql.util.TreeTransformer;

import static graphql.Assert.assertNotNull;
import static graphql.nadel.result.ResultNodeAdapter.RESULT_NODE_ADAPTER;

@Internal
public class ResultNodesTransformer {

    public ExecutionResultNode transform(ExecutionResultNode root, TraverserVisitor<ExecutionResultNode> traverserVisitor) {
        assertNotNull(root);

        TreeTransformer<ExecutionResultNode> treeTransformer = new TreeTransformer<>(RESULT_NODE_ADAPTER);
        return treeTransformer.transform(root, traverserVisitor);
    }

//    public ExecutionResultNode transformParallel(ForkJoinPool forkJoinPool, ExecutionResultNode root, TraverserVisitor<ExecutionResultNode> traverserVisitor) {
//        return transformParallel(forkJoinPool, root, traverserVisitor, Collections.emptyMap());
//    }
//
//    public ExecutionResultNode transformParallel(ForkJoinPool forkJoinPool, ExecutionResultNode root, TraverserVisitor<ExecutionResultNode> traverserVisitor, Map<Class<?>, Object> rootVars) {
//        assertNotNull(root);
//
//        TreeParallelTransformer<ExecutionResultNode> parallelTransformer = TreeParallelTransformer.parallelTransformer(RESULT_NODE_ADAPTER, forkJoinPool);
//        parallelTransformer.rootVars(rootVars);
//        return parallelTransformer.transform(root, traverserVisitor);
//    }
}

