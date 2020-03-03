package graphql.nadel.engine.transformation;

import graphql.GraphQLError;
import graphql.language.Field;
import graphql.nadel.normalized.NormalizedQueryField;

import java.util.List;

public class RemovedFieldData {
    private final Field field;
    private final GraphQLError graphQLError;
    private final List<NormalizedQueryField> normalizedQueryFields;

    public RemovedFieldData(Field field, GraphQLError graphQLError, List<NormalizedQueryField> normalizedQueryFields) {
        this.field = field;
        this.graphQLError = graphQLError;
        this.normalizedQueryFields = normalizedQueryFields;
    }

    public Field getField() {
        return field;
    }

    public GraphQLError getGraphQLError() {
        return graphQLError;
    }

    public List<NormalizedQueryField> getNormalizedQueryFields() {
        return normalizedQueryFields;
    }
}
