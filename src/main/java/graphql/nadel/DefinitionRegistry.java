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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public Map<OperationType, ObjectTypeDefinition> getOperationTypes(){
        Map<OperationType, ObjectTypeDefinition > opsTypesMap = new HashMap<>();

        Arrays.stream(OperationType.values()).forEach(opsType ->
                opsTypesMap.put(opsType, getOpsType(opsType.getOpsType(), opsType.getDisplayName())));;

       return opsTypesMap;
    }

    public ObjectTypeDefinition getQueryType() {
        return getOpsType(OperationType.QUERY.getOpsType(), OperationType.QUERY.getDisplayName());
    }

    private ObjectTypeDefinition getOpsType(String typeName, String typeFlag ) {
        SchemaDefinition schemaDefinition = getSchemaDefinition();
        if (schemaDefinition != null) {
            Optional<OperationTypeDefinition> queryOp = schemaDefinition.getOperationTypeDefinitions().stream().filter(op -> typeName.equalsIgnoreCase(op.getName())).findFirst();
            if (!queryOp.isPresent()) {
                return null;
            }
            String queryName = queryOp.get().getTypeName().getName();
            return getDefinition(queryName, ObjectTypeDefinition.class);
        }
        return getDefinition(typeFlag, ObjectTypeDefinition.class);
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
