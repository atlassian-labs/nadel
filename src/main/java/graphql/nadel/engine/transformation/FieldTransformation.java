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

    default ExecutionResultNode unapplyResultNode(ExecutionResultNode executionResultNode) {
        return null;
    }

    String NADEL_FIELD_ID = "FIELD_ID";
}
