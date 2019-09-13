package graphql.nadel.engine.transformation;

import graphql.util.TraversalControl;

public class ApplyResult {
    private final TraversalControl traversalControl;

    public ApplyResult(TraversalControl traversalControl) {
        this.traversalControl = traversalControl;
    }

    public TraversalControl getTraversalControl() {
        return traversalControl;
    }
}

