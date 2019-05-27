package graphql.nadel.schema;

import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.SDLDefinition;
import graphql.language.SchemaDefinition;
import graphql.nadel.DefinitionRegistry;
import graphql.nadel.Operation;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.WiringFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static graphql.language.ObjectTypeDefinition.newObjectTypeDefinition;

public class OverallSchemaGenerator {


    public GraphQLSchema buildOverallSchema(List<DefinitionRegistry> serviceRegistries, DefinitionRegistry commonTypes, WiringFactory wiringFactory) {
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .wiringFactory(wiringFactory)
                .build();
        return schemaGenerator.makeExecutableSchema(createTypeRegistry(serviceRegistries, commonTypes), runtimeWiring);
    }

    private TypeDefinitionRegistry createTypeRegistry(List<DefinitionRegistry> serviceRegistries, DefinitionRegistry commonTypes) {
        //TODO: this merging not completely correct for example schema definition nodes are not handled correctly
        Map<Operation, List<FieldDefinition>> fieldsMapByType = new HashMap<>();
        Arrays.stream(Operation.values()).forEach(
                value -> fieldsMapByType.put(value, new ArrayList<>()));

        TypeDefinitionRegistry overallRegistry = new TypeDefinitionRegistry();
        List<SDLDefinition> allDefinitions = new ArrayList<>();

        for (DefinitionRegistry definitionRegistry : serviceRegistries) {
            collectTypes(fieldsMapByType, allDefinitions, definitionRegistry);
        }
        collectTypes(fieldsMapByType, allDefinitions, commonTypes);

        fieldsMapByType.keySet().forEach(key -> {
            overallRegistry.add(newObjectTypeDefinition().name(key.getDisplayName()).fieldDefinitions(fieldsMapByType.get(key)).build());
        });

        allDefinitions.forEach(overallRegistry::add);
        return overallRegistry;
    }

    private void collectTypes(Map<Operation, List<FieldDefinition>> fieldsMapByType, List<SDLDefinition> allDefinitions, DefinitionRegistry definitionRegistry) {
        Map<Operation, ObjectTypeDefinition> opsTypes = definitionRegistry.getOperationMap();
        opsTypes.keySet().forEach(opsType -> {
            ObjectTypeDefinition opsDefinitions = opsTypes.get(opsType);
            if (opsDefinitions != null) {
                fieldsMapByType.get(opsType).addAll(opsDefinitions.getFieldDefinitions());
            }
            definitionRegistry
                    .getDefinitions()
                    .stream()
                    .filter(sdlDefinition -> !(sdlDefinition instanceof SchemaDefinition) && sdlDefinition != opsDefinitions)
                    .forEach(allDefinitions::add);
        });
    }

}
