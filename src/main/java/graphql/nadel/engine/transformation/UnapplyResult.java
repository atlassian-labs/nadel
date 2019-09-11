package graphql.nadel.engine.transformation;

import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.util.TraversalControl;

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
