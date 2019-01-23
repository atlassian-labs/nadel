package graphql.nadel.engine.transformation;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.util.TraversalControl;

import static graphql.nadel.engine.transformation.FieldUtils.resultKeyForField;

public class CopyFieldTransformation implements FieldTransformation {
    protected String resultKey;

    @Override
    public TraversalControl apply(QueryVisitorFieldEnvironment environment) {
        resultKey = resultKeyForField(environment.getField());
        // Not changing node means it will be preserved as is
        return TraversalControl.CONTINUE;
    }
}
