package graphql.nadel;

import graphql.Internal;
import graphql.schema.GraphQLFieldDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

@Internal
public class FieldInfos {

    private Map<GraphQLFieldDefinition, FieldInfo> fieldInfoByDefinition = new LinkedHashMap<>();

    public FieldInfo getInfo(GraphQLFieldDefinition fieldDefinition) {
        return fieldInfoByDefinition.get(fieldDefinition);
    }

}
