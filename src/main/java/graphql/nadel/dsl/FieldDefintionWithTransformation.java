package graphql.nadel.dsl;

import graphql.language.FieldDefinition;
import graphql.language.Type;

public class FieldDefintionWithTransformation extends FieldDefinition {

    public FieldDefintionWithTransformation(String name, Type type) {
        super(name, type);
    }
}
