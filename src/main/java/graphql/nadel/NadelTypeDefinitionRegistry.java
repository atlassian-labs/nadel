package graphql.nadel;

import graphql.Internal;
import graphql.language.ObjectTypeExtensionDefinition;
import graphql.language.Type;
import graphql.language.TypeDefinition;
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

@Internal
public class NadelTypeDefinitionRegistry {

    private Map<ServiceDefinition, TypeDefinitionRegistry> typeDefinitionRegistries = new LinkedHashMap<>();
    private StitchingDsl stitchingDsl;

    public NadelTypeDefinitionRegistry(StitchingDsl stitchingDsl) {
        this.stitchingDsl = stitchingDsl;
        List<ServiceDefinition> serviceDefinitions = stitchingDsl.getServiceDefinitions();
        for (ServiceDefinition serviceDefinition : serviceDefinitions) {
            List<TypeDefinition<?>> typeDefinitions = serviceDefinition.getTypeDefinitions();
            TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();
            typeDefinitionRegistries.put(serviceDefinition, typeDefinitionRegistry);

            for (TypeDefinition typeDefinition : typeDefinitions) {
                typeDefinitionRegistry.add(typeDefinition);
            }
        }
    }


    public Map<String, List<ObjectTypeExtensionDefinition>> typeExtensions() {
        Map<String, List<ObjectTypeExtensionDefinition>> result = new LinkedHashMap<>();
        for (TypeDefinitionRegistry typeDefinitionRegistry : typeDefinitionRegistries.values()) {
            Map<String, List<ObjectTypeExtensionDefinition>> typeExtensions = typeDefinitionRegistry.objectTypeExtensions();
            for (Map.Entry<String, List<ObjectTypeExtensionDefinition>> extensionEntry : typeExtensions.entrySet()) {
                result.putIfAbsent(extensionEntry.getKey(), new ArrayList<>());
                result.get(extensionEntry.getKey()).addAll(extensionEntry.getValue());
            }
        }
        return result;
    }

    public Optional<TypeDefinition> getTypeDefinitions(Type type) {
        String typeName = TypeInfo.typeInfo(type).getName();
        return getTypeDefinition(typeName);
    }

    public Optional<TypeDefinition> getTypeDefinition(String name) {
        List<TypeDefinition> typeDefinitions = getTypeDefinitions(name);
        return typeDefinitions.size() > 0 ? Optional.of(typeDefinitions.get(0)) : Optional.empty();
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

    public StitchingDsl getStitchingDsl() {
        return stitchingDsl;
    }
}
