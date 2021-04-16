package graphql.nadel.engine.execution.transformation;

import graphql.Internal;
import graphql.nadel.engine.result.ExecutionResultNode;
import graphql.util.TraversalControl;

@Internal
public class UnapplyResult {

    private final ExecutionResultNode node;
    private final TraversalControl traversalControl;

    public UnapplyResult(ExecutionResultNode node, TraversalControl traversalControl) {
        this.node = node;
        this.traversalControl = traversalControl;
    }

    public ExecutionResultNode getNode() {
        return node;
    }

    public TraversalControl getTraversalControl() {
        return traversalControl;
    }
}
