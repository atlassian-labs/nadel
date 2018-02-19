package graphql.nadel;

import graphql.Internal;
import graphql.language.Type;
import graphql.language.TypeDefinition;
import graphql.language.TypeExtensionDefinition;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.nadel.dsl.StitchingDsl;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static graphql.Assert.assertTrue;

@Internal
public class NadelTypeDefinitionRegistry {

    private Map<ServiceDefinition, TypeDefinitionRegistry> typeDefinitionRegistries = new LinkedHashMap<>();


    public NadelTypeDefinitionRegistry(StitchingDsl stitchingDsl) {
        List<ServiceDefinition> serviceDefinitions = stitchingDsl.getServiceDefinitions();
        for (ServiceDefinition serviceDefinition : serviceDefinitions) {
            List<TypeDefinition<?>> typeDefinitions = serviceDefinition.getTypeDefinitions();
            TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();

            for (TypeDefinition typeDefinition : typeDefinitions) {
                typeDefinitionRegistry.add(typeDefinition);
            }
        }
    }

    public Optional<TypeDefinition> getType(Type type) {
        String typeName = TypeInfo.typeInfo(type).getName();
        return getType(typeName);
    }

    public Optional<TypeDefinition> getType(String name) {
        List<TypeDefinition> typeDefinitions = getTypeDefinitions(name);
        assertTrue(typeDefinitions.size() <= 1, "more than one type found with name " + name);
        return typeDefinitions.size() > 0 ? Optional.of(typeDefinitions.get(0)) : Optional.empty();
    }

    public Map<String, List<TypeExtensionDefinition>> typeExtensions() {
        Map<String, List<TypeExtensionDefinition>> result = new LinkedHashMap<>();
        for (TypeDefinitionRegistry typeDefinitionRegistry : typeDefinitionRegistries.values()) {
            Map<String, List<TypeExtensionDefinition>> typeExtensions = typeDefinitionRegistry.typeExtensions();
            for (Map.Entry<String, List<TypeExtensionDefinition>> extensionEntry : typeExtensions.entrySet()) {
                result.putIfAbsent(extensionEntry.getKey(), new ArrayList<>());
                result.get(extensionEntry.getKey()).addAll(extensionEntry.getValue());
            }
        }
        return result;
    }


    public List<TypeDefinition> getTypeDefinitions(String name) {
        return typeDefinitionRegistries.values().stream()
                .map(typeDefinitionRegistry -> typeDefinitionRegistry.getType(name))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public List<TypeDefinition> getAllTypeDefinitions() {
        return typeDefinitionRegistries.values().stream()
                .flatMap(typeDefinitionRegistry -> typeDefinitionRegistry.types().values().stream())
                .collect(Collectors.toList());
    }

}
