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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Internal
public class DefinitionRegistry {

    private List<SDLDefinition> definitions = new ArrayList<>();
    private Map<Class<? extends SDLDefinition>, List<SDLDefinition>> definitionsByClass = new LinkedHashMap<>();
    private Map<String, List<SDLDefinition>> definitionsByName = new LinkedHashMap<>();

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

    public Map<Operation, List<ObjectTypeDefinition>> getOperationMap() {
        return Stream.of(Operation.values()).collect(HashMap::new, (m, v) -> m.put(v, getOpsDefinitions(v)), HashMap::putAll);
    }

    public List<ObjectTypeDefinition> getQueryType() {
        return getOpsDefinitions(Operation.QUERY);
    }

    public List<ObjectTypeDefinition> getMutationType() {
        return getOpsDefinitions(Operation.MUTATION);
    }

    private List<ObjectTypeDefinition> getOpsDefinitions(Operation operation) {
        String type = getOperationTypeName(operation);
        return getDefinition(type, ObjectTypeDefinition.class);
    }

    public String getOperationTypeName(Operation operation) {
        String operationName = operation.getName(); // e.g. query, mutation etc.

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
        return operation.getDisplayName();
    }

    private <T extends SDLDefinition> List<T> getDefinition(String name, Class<? extends T> clazz) {
        List<SDLDefinition> sdlDefinitions = definitionsByName.get(name);
        if (sdlDefinitions == null) {
            return null;
        }
        List<SDLDefinition> result = FpKit.filter(sdlDefinitions, clazz::isInstance);
        return (List<T>) result;
    }

    public List<SDLDefinition> getDefinitions() {
        return definitions;
    }

}
