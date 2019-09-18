package graphql.nadel.engine.transformation;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;

public class FieldTypeInfo {
    private final GraphQLFieldsContainer fieldsContainer;
    private final GraphQLFieldDefinition fieldDefinition;

    public FieldTypeInfo(GraphQLFieldsContainer fieldsContainer, GraphQLFieldDefinition fieldDefinition) {
        this.fieldsContainer = fieldsContainer;
        this.fieldDefinition = fieldDefinition;
    }

    public GraphQLFieldsContainer getFieldsContainer() {
        return fieldsContainer;
    }


    public GraphQLFieldDefinition getFieldDefinition() {
        return fieldDefinition;
    }
}

