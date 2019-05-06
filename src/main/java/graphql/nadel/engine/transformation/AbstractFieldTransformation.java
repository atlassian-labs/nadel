package graphql.nadel.engine.transformation;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.language.Field;
import graphql.schema.GraphQLOutputType;
import graphql.util.TraversalControl;

import static graphql.nadel.engine.transformation.FieldUtils.resultKeyForField;

public abstract class AbstractFieldTransformation implements FieldTransformation {

    private String resultKey;
    private QueryVisitorFieldEnvironment environment;

    @Override
    public TraversalControl apply(QueryVisitorFieldEnvironment environment) {
        this.environment = environment;
        resultKey = resultKeyForField(environment.getField());
        // Not changing node means it will be preserved as is
        return TraversalControl.CONTINUE;
    }

    @Override
    public Field unapplyField(Field field) {
        return getOriginalField();
    }

    public String getResultKey() {
        return resultKey;
    }

    public QueryVisitorFieldEnvironment getOriginalFieldEnvironment() {
        return environment;
    }

    public Field getOriginalField() {
        return getOriginalFieldEnvironment().getField();
    }

    public GraphQLOutputType getOriginalFieldType() {
        return getOriginalFieldEnvironment().getFieldDefinition().getType();
    }

}
