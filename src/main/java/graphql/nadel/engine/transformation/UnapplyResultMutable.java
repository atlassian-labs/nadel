package graphql.nadel.engine.transformation;

import graphql.nadel.execution.ExecutionResultNode;
import graphql.util.TraversalControl;

public class UnapplyResultMutable {

    private final ExecutionResultNode node;
    private final TraversalControl traversalControl;

    public UnapplyResultMutable(ExecutionResultNode node, TraversalControl traversalControl) {
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
