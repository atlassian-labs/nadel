package graphql.nadel.engine.transformation;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.MergedField;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.util.TraversalControl;

public interface FieldTransformation {

    TraversalControl apply(QueryVisitorFieldEnvironment environment);

    default MergedField unapplyMergedField(MergedField mergedField) {
        return mergedField;
    }

    default <T extends ExecutionResultNode> T unapplyResultNode(T executionResultNode) {
        return executionResultNode;
    }
}
