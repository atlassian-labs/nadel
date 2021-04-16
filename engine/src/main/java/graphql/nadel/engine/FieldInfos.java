package graphql.nadel.engine;

import graphql.Internal;
import graphql.schema.GraphQLFieldDefinition;

import java.util.Map;

@Internal
public class FieldInfos {

    private final Map<GraphQLFieldDefinition, FieldInfo> fieldInfoByDefinition;

    public FieldInfos(Map<GraphQLFieldDefinition, FieldInfo> fieldInfoByDefinition) {
        this.fieldInfoByDefinition = fieldInfoByDefinition;
    }

    public FieldInfo getInfo(GraphQLFieldDefinition fieldDefinition) {
        return fieldInfoByDefinition.get(fieldDefinition);
    }

}
