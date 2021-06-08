package graphql.nadel.engine;

import graphql.Internal;
import graphql.nadel.Service;
import graphql.schema.GraphQLFieldDefinition;

@Internal
public class FieldInfo {

    public enum FieldKind {
        /**
         * A top level field is declared directly under the GraphQL operation types: query, mutation and subscription
         */
        TOPLEVEL
    }

    private final FieldKind fieldKind;
    private final Service service;
    private final GraphQLFieldDefinition fieldDefinition;

    public FieldInfo(FieldKind fieldKind, Service service, GraphQLFieldDefinition fieldDefinition) {
        this.fieldKind = fieldKind;
        this.service = service;
        this.fieldDefinition = fieldDefinition;
    }

    public FieldKind getFieldKind() {
        return fieldKind;
    }

    public Service getService() {
        return service;
    }

    public GraphQLFieldDefinition getFieldDefinition() {
        return fieldDefinition;
    }
}
