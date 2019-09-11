package graphql.nadel.engine.transformation;

import graphql.schema.GraphQLOutputType;
import graphql.util.TraversalControl;

public class ApplyResult {
    private final TraversalControl traversalControl;
    private final GraphQLOutputType newParentTypeUnderlying;


    public ApplyResult(TraversalControl traversalControl, GraphQLOutputType newParentTypeUnderlying) {
        this.traversalControl = traversalControl;
        this.newParentTypeUnderlying = newParentTypeUnderlying;
    }

    public TraversalControl getTraversalControl() {
        return traversalControl;
    }

    public GraphQLOutputType getNewParentTypeUnderlying() {
        return newParentTypeUnderlying;
    }
}
