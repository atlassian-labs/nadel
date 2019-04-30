package graphql.nadel.engine.transformation;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.language.Field;
import graphql.util.TraversalControl;

public interface FieldTransformation {

    TraversalControl apply(QueryVisitorFieldEnvironment environment);

    default Field unapplyField(Field field) {
        return field;
    }

    default <T extends ExecutionResultNode> T unapplyResultNode(T executionResultNode) {
        return executionResultNode;
    }
}
