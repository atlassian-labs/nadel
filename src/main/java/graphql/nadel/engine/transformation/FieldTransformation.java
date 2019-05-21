package graphql.nadel.engine.transformation;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.language.Field;
import graphql.nadel.engine.UnapplyEnvironment;
import graphql.util.TraversalControl;

import java.util.List;

public interface FieldTransformation {

    String NADEL_FIELD_ID = "FIELD_ID";

    TraversalControl apply(QueryVisitorFieldEnvironment environment);

    /**
     * This is a bit strange method because n FieldTransformations map to one unapply method and we don't know the mapping until
     * this method is called. So we actually give all relevant transformations as a List
     */
    ExecutionResultNode unapplyResultNode(ExecutionResultNode executionResultNode,
                                          List<FieldTransformation> allTransformations,
                                          UnapplyEnvironment environment);

    Field getOriginalField();
}
