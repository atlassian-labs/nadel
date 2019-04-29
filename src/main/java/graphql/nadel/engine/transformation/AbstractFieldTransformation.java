package graphql.nadel.engine.transformation;

import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.MergedField;
import graphql.language.Field;
import graphql.schema.GraphQLOutputType;
import graphql.util.FpKit;
import graphql.util.TraversalControl;

import java.util.List;

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
    public MergedField unapplyMergedField(MergedField mergedField) {
        String originalName = getOriginalField().getName();
        List<Field> fields = FpKit.map(mergedField.getFields(), field -> field.transform(builder -> builder.name(originalName)));
        return MergedField.newMergedField(fields).build();
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
