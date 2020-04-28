package graphql.nadel.engine.transformation;

import graphql.Internal;
import graphql.util.TraversalControl;

@Internal
public class ApplyResult {
    private final TraversalControl traversalControl;

    public ApplyResult(TraversalControl traversalControl) {
        this.traversalControl = traversalControl;
    }

    public TraversalControl getTraversalControl() {
        return traversalControl;
    }
}

