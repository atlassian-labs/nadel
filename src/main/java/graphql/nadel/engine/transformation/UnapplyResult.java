package graphql.nadel.engine.transformation;

import graphql.Internal;
import graphql.nadel.result.ExecutionResultNode;
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
