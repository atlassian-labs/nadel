package graphql.nadel;

import graphql.Internal;
import graphql.language.DirectiveDefinition;
import graphql.language.NamedNode;
import graphql.language.ObjectTypeDefinition;
import graphql.language.OperationTypeDefinition;
import graphql.language.SDLDefinition;
import graphql.language.SchemaDefinition;
import graphql.language.TypeDefinition;
import graphql.nadel.util.FpKit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

/**
 * Alternative to {@link graphql.schema.idl.TypeDefinitionRegistry} but is more generic
 * and tailored to Nadel specific operations to build the overall schema.
 */
@Internal
public class NadelDefinitionRegistry {
    private final List<SDLDefinition> definitions = new ArrayList<>();
    private final Map<Class<? extends SDLDefinition>, List<SDLDefinition>> definitionsByClass = new LinkedHashMap<>();
    private final Map<String, List<SDLDefinition>> definitionsByName = new LinkedHashMap<>();

    public static NadelDefinitionRegistry from(List<SDLDefinition> definitions) {
        NadelDefinitionRegistry registry = new NadelDefinitionRegistry();
        for (SDLDefinition<?> definition : definitions) {
            registry.add(definition);
        }
        return registry;
    }

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
        Map<OperationKind, List<ObjectTypeDefinition>> operationMap = new LinkedHashMap<>();

        for (OperationKind operationKind : OperationKind.values()) {
            operationMap.put(operationKind, getOpsDefinitions(operationKind));
        }

        return operationMap;
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
        return operationKind.getDefaultTypeName();
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

    @NotNull
    public <T extends SDLDefinition> List<T> getDefinitions(Class<T> targetClass) {
        return (List<T>) FpKit.filter(getDefinitions(), targetClass::isInstance);
    }
}
