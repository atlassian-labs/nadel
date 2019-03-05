package graphql.nadel;

import graphql.Internal;
import graphql.language.DirectiveDefinition;
import graphql.language.NamedNode;
import graphql.language.ObjectTypeDefinition;
import graphql.language.OperationTypeDefinition;
import graphql.language.SDLDefinition;
import graphql.language.SchemaDefinition;
import graphql.language.TypeDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public Map<Operation, ObjectTypeDefinition> getOperationMap() {
        return Stream.of(Operation.values()).collect(HashMap::new, (m, v) -> m.put(v, getOpsDefinitions(v.getName(), v.getDisplayName())), HashMap::putAll);
    }

    public ObjectTypeDefinition getQueryType() {
        return getOpsDefinitions(Operation.QUERY.getName(), Operation.QUERY.getDisplayName());
    }

    public ObjectTypeDefinition getMutationType() {
        return getOpsDefinitions(Operation.MUTATION.getName(), Operation.MUTATION.getDisplayName());
    }

    private ObjectTypeDefinition getOpsDefinitions(String typeName, String typeDisplay) {
        SchemaDefinition schemaDefinition = getSchemaDefinition();
        if (schemaDefinition != null) {
            Optional<OperationTypeDefinition> opDefinitionsOp = schemaDefinition.getOperationTypeDefinitions().stream()
                    .filter(op -> typeName.equalsIgnoreCase(op.getName())).findFirst();
            if (!opDefinitionsOp.isPresent()) {
                return null;
            }
            String operationName = opDefinitionsOp.get().getTypeName().getName();
            return getDefinition(operationName, ObjectTypeDefinition.class);
        }
        return getDefinition(typeDisplay, ObjectTypeDefinition.class);
    }

    private <T extends SDLDefinition> T getDefinition(String name, Class<? extends T> clazz) {
        List<SDLDefinition> sdlDefinitions = definitionsByName.get(name);
        if (sdlDefinitions == null) {
            return null;
        }
        Optional<SDLDefinition> result = sdlDefinitions.stream().filter(clazz::isInstance).findFirst();
        return (T) result.orElse(null);
    }

    public List<SDLDefinition> getDefinitions() {
        return definitions;
    }

}
