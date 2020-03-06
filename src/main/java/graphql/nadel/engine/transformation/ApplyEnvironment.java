package graphql.nadel.engine.transformation;

import graphql.language.Field;
import graphql.language.Node;
import graphql.nadel.normalized.NormalizedQueryField;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.util.TraverserContext;

import java.util.List;

public class ApplyEnvironment {
    private final Field field;
    private final GraphQLFieldDefinition fieldDefinitionOverall;
    private final GraphQLFieldsContainer fieldsContainerOverall;
    private final TraverserContext<Node> traverserContext;
    private List<NormalizedQueryField> normalizedQueryFieldsOverall;

    public ApplyEnvironment(Field field,
                            GraphQLFieldDefinition fieldDefinitionOverall,
                            GraphQLFieldsContainer fieldsContainerOverall,
                            TraverserContext<Node> traverserContext,
                            List<NormalizedQueryField> normalizedQueryFieldsOverall) {
        this.field = field;
        this.fieldDefinitionOverall = fieldDefinitionOverall;
        this.fieldsContainerOverall = fieldsContainerOverall;
        this.traverserContext = traverserContext;
        this.normalizedQueryFieldsOverall = normalizedQueryFieldsOverall;
    }

    public Field getField() {
        return field;
    }

    public GraphQLFieldDefinition getFieldDefinitionOverall() {
        return fieldDefinitionOverall;
    }

    public GraphQLFieldsContainer getFieldsContainerOverall() {
        return fieldsContainerOverall;
    }

    public TraverserContext<Node> getTraverserContext() {
        return traverserContext;
    }

    public List<NormalizedQueryField> getNormalizedQueryFieldsOverall() {
        return normalizedQueryFieldsOverall;
    }
}
