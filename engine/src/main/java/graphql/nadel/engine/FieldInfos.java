package graphql.nadel.engine;

import graphql.Internal;
import graphql.nadel.Service;
import graphql.schema.GraphQLFieldDefinition;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Internal
public class FieldInfos {

    private final Map<GraphQLFieldDefinition, FieldInfo> fieldInfoByDefinition;
    public final Map<Service, Set<GraphQLFieldDefinition>> fieldDefinitionsByService;

    public FieldInfos(Map<GraphQLFieldDefinition, FieldInfo> fieldInfoByDefinition) {
        this.fieldInfoByDefinition = fieldInfoByDefinition;
        this.fieldDefinitionsByService = fieldInfoByDefinition.entrySet()
                .stream()
                .collect(Collectors.groupingBy(
                        graphQLFieldDefinitionFieldInfoEntry -> graphQLFieldDefinitionFieldInfoEntry.getValue().getService(),
                        Collectors.mapping(Map.Entry::getKey, Collectors.toSet())));

    }

    public FieldInfo getInfo(GraphQLFieldDefinition fieldDefinition) {
        return fieldInfoByDefinition.get(fieldDefinition);
    }

}
