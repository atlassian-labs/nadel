package graphql.nadel;

import graphql.Internal;
import graphql.language.DirectiveDefinition;
import graphql.language.NamedNode;
import graphql.language.ObjectTypeDefinition;
import graphql.language.OperationTypeDefinition;
import graphql.language.SDLDefinition;
import graphql.language.SchemaDefinition;
import graphql.language.TypeDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

@Internal
public class DefinitionRegistry {
    private final List<SDLDefinition> definitions = new ArrayList<>();
    private final Map<Class<? extends SDLDefinition>, List<SDLDefinition>> definitionsByClass = new LinkedHashMap<>();
    private final Map<String, List<SDLDefinition>> definitionsByName = new LinkedHashMap<>();

    public void add(SDLDefinition sdlDefinition) {
        definitions.add(sdlDefinition);
        definitionsByClass.computeIfAbsent(sdlDefinition.getClass(), key -> new ArrayList<>());
        definitionsByClass.get(sdlDefinition.getClass()).add(sdlDefinition);

        if (sdlDefinition instanceof TypeDefinition || sdlDefinition instanceof DirectiveDefinition) {
            String name = ((NamedNode) sdlDefinition).getName();
            definitionsByName.computeIfAbsent(name, key -> new ArrayList<>());
            definitionsByName.get(name).add(sdlDefinition);
        }
    }

    public SchemaDefinition getSchemaDefinition() {
        if (!definitionsByClass.containsKey(SchemaDefinition.class)) {
            return null;
        }
        return (SchemaDefinition) definitionsByClass.get(SchemaDefinition.class).get(0);
    }

    public Map<OperationKind, List<ObjectTypeDefinition>> getOperationMap() {
        return Stream.of(OperationKind.values()).collect(HashMap::new, (m, v) -> m.put(v, getOpsDefinitions(v)), HashMap::putAll);
    }

    @NotNull
    public List<ObjectTypeDefinition> getQueryType() {
        return getOpsDefinitions(OperationKind.QUERY);
    }

    @NotNull
    public List<ObjectTypeDefinition> getMutationType() {
        return getOpsDefinitions(OperationKind.MUTATION);
    }

    @NotNull
    public List<ObjectTypeDefinition> getSubscriptionType() {
        return getOpsDefinitions(OperationKind.SUBSCRIPTION);
    }

    @NotNull
    private List<ObjectTypeDefinition> getOpsDefinitions(OperationKind operationKind) {
        String type = getOperationTypeName(operationKind);
        return getDefinition(type, ObjectTypeDefinition.class);
    }

    @Nullable
    public String getOperationTypeName(OperationKind operationKind) {
        String operationName = operationKind.getName(); // e.g. query, mutation etc.

        // Check the schema definition for the operation type
        // i.e. we are trying to find MyOwnQueryType in: schema { query: MyOwnQueryType }
        SchemaDefinition schemaDefinition = getSchemaDefinition();
        if (schemaDefinition != null) {
            for (OperationTypeDefinition opTypeDef : schemaDefinition.getOperationTypeDefinitions()) {
                if (opTypeDef.getName().equalsIgnoreCase(operationName)) {
                    return opTypeDef.getTypeName().getName();
                }
            }
            return null;
        }

        // This is the default name if there is no schema definition
        return operationKind.getDisplayName();
    }

    @NotNull
    private <T extends SDLDefinition> List<T> getDefinition(String name, Class<? extends T> clazz) {
        List<SDLDefinition> sdlDefinitions = definitionsByName.get(name);
        if (sdlDefinitions == null) {
            return emptyList();
        }
        List<SDLDefinition> result = sdlDefinitions.stream().filter(clazz::isInstance).collect(Collectors.toList());
        return (List<T>) result;
    }

    @NotNull
    public List<SDLDefinition> getDefinitions() {
        return definitions;
    }
}
