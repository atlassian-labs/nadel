package graphql.nadel.engine;

import graphql.Assert;
import graphql.Internal;
import graphql.VisibleForTesting;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.nadel.Service;
import graphql.nadel.util.FpKit;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static graphql.nadel.schema.NadelDirectives.NAMESPACED_DIRECTIVE_DEFINITION;

@Internal
public class FieldInfos {

    private final Map<GraphQLFieldDefinition, FieldInfo> fieldInfoByDefinition;

    @VisibleForTesting
    public FieldInfos(Map<GraphQLFieldDefinition, FieldInfo> fieldInfoByDefinition) {
        this.fieldInfoByDefinition = fieldInfoByDefinition;
    }

    public static FieldInfos createFieldsInfos(GraphQLSchema overallSchema, Collection<Service> services) {
        Map<GraphQLFieldDefinition, FieldInfo> fieldInfoByDefinition = new LinkedHashMap<>();

        Set<GraphQLOutputType> namespacedGraphqlTypes = Stream.of(
                overallSchema.getQueryType(),
                overallSchema.getMutationType()
        )
                .filter(Objects::nonNull)
                .flatMap(graphQLObjectType -> graphQLObjectType.getFieldDefinitions().stream())
                .filter(topLevelFieldDef -> !topLevelFieldDef.getDirectives(NAMESPACED_DIRECTIVE_DEFINITION.getName()).isEmpty())
                .map(GraphQLFieldDefinition::getType)
                .collect(Collectors.toSet());

        if (!namespacedGraphqlTypes.isEmpty()) {
            extractNamespacedFieldInfos(fieldInfoByDefinition, namespacedGraphqlTypes, overallSchema, services);
        }

        GraphQLObjectType schemaQueryType = overallSchema.getQueryType();
        GraphQLObjectType schemaMutationType = overallSchema.getMutationType();
        GraphQLObjectType schemaSubscriptionType = overallSchema.getSubscriptionType();
        for (Service service : services) {
            List<ObjectTypeDefinition> queryTypeDefinitions = service.getDefinitionRegistry().getQueryType();
            updateFieldInfoMap(fieldInfoByDefinition, schemaQueryType, service, queryTypeDefinitions);

            List<ObjectTypeDefinition> mutationTypeDefinitions = service.getDefinitionRegistry().getMutationType();
            updateFieldInfoMap(fieldInfoByDefinition, schemaMutationType, service, mutationTypeDefinitions);

            List<ObjectTypeDefinition> subscriptionTypeDefinitions = service.getDefinitionRegistry().getSubscriptionType();
            updateFieldInfoMap(fieldInfoByDefinition, schemaSubscriptionType, service, subscriptionTypeDefinitions);
        }

        return new FieldInfos(fieldInfoByDefinition);
    }

    private static void updateFieldInfoMap(Map<GraphQLFieldDefinition, FieldInfo> fieldInfoByDefinition, GraphQLObjectType schemaQueryType, Service service, List<ObjectTypeDefinition> queryType) {
        for (ObjectTypeDefinition objectTypeDefinition : queryType) {
            for (FieldDefinition fieldDefinition : objectTypeDefinition.getFieldDefinitions()) {
                GraphQLFieldDefinition graphQLFieldDefinition = schemaQueryType.getFieldDefinition(fieldDefinition.getName());
                FieldInfo fieldInfo = new FieldInfo(FieldInfo.FieldKind.TOPLEVEL, service, graphQLFieldDefinition);
                fieldInfoByDefinition.put(graphQLFieldDefinition, fieldInfo);
            }
        }
    }

    private static void extractNamespacedFieldInfos(
            Map<GraphQLFieldDefinition, FieldInfo> fieldInfoByDefinition, Set<GraphQLOutputType> namespacedGraphqlTypes, GraphQLSchema overallSchema, Collection<Service> services
    ) {
        for (Service service : services) {
            for (ObjectTypeDefinition typeDefinition : service.getDefinitionRegistry().getDefinitions(ObjectTypeDefinition.class)) {
                boolean isNamespacedDefinition = namespacedGraphqlTypes.stream()
                        .filter(type -> type instanceof GraphQLNamedType)
                        .map(type -> (GraphQLNamedType) type)
                        .anyMatch(type -> type.getName().equals(typeDefinition.getName()));
                if (!isNamespacedDefinition) {
                    continue;
                }
                GraphQLObjectType namespacedGraphQLObjectType = overallSchema.getObjectType(typeDefinition.getName());
                List<GraphQLFieldDefinition> graphQLFieldDefinitionsWithinNamespacedType = namespacedGraphQLObjectType.getFieldDefinitions();

                List<FieldDefinition> serviceFieldDefinitionsWithinNamespacedType = typeDefinition.getFieldDefinitions();

                for (FieldDefinition serviceSecondLevelFieldDefinition : serviceFieldDefinitionsWithinNamespacedType) {
                    GraphQLFieldDefinition secondLeveGraphqlFieldDefinition = FpKit.findOneOrNull(
                            graphQLFieldDefinitionsWithinNamespacedType,
                            gqlDef -> gqlDef.getName().equals(serviceSecondLevelFieldDefinition.getName())
                    );

                    Assert.assertNotNull(secondLeveGraphqlFieldDefinition,
                            () -> "field definition for a field in a namespaced type is not found in the overall schema");

                    FieldInfo fieldInfo = new FieldInfo(FieldInfo.FieldKind.NAMESPACE_SUBFIELD, service, secondLeveGraphqlFieldDefinition);
                    fieldInfoByDefinition.put(secondLeveGraphqlFieldDefinition, fieldInfo);
                }
            }
        }
    }

    public FieldInfo getInfo(GraphQLFieldDefinition fieldDefinition) {
        return fieldInfoByDefinition.get(fieldDefinition);
    }

    public Map<Service, Set<GraphQLFieldDefinition>> splitObjectFieldsByServices(GraphQLObjectType namespacedObjectType) {
        return namespacedObjectType.getFieldDefinitions()
                .stream()
                .collect(Collectors.groupingBy(
                        fieldDefinition -> getInfo(fieldDefinition).getService(),
                        Collectors.toSet()));
    }
}
