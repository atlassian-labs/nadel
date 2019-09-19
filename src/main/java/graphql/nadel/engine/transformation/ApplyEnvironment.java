package graphql.nadel.engine.transformation;

import graphql.language.Field;
import graphql.language.Node;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.util.TraverserContext;

public class ApplyEnvironment {
    private final Field field;
    private final GraphQLFieldDefinition fieldDefinition;
    private final GraphQLFieldsContainer fieldsContainer;
    private final TraverserContext<Node> traverserContext;

    public ApplyEnvironment(Field field, GraphQLFieldDefinition fieldDefinition, GraphQLFieldsContainer fieldsContainer, TraverserContext<Node> traverserContext) {
        this.field = field;
        this.fieldDefinition = fieldDefinition;
        this.fieldsContainer = fieldsContainer;
        this.traverserContext = traverserContext;
    }

    public Field getField() {
        return field;
    }

    public GraphQLFieldDefinition getFieldDefinition() {
        return fieldDefinition;
    }

    public GraphQLFieldsContainer getFieldsContainer() {
        return fieldsContainer;
    }

    public TraverserContext<Node> getTraverserContext() {
        return traverserContext;
    }
}
