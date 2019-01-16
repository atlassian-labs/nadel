package graphql.nadel;

import graphql.Internal;
import graphql.language.ObjectTypeDefinition;
import graphql.language.OperationTypeDefinition;
import graphql.language.TypeName;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.List;

@Internal
public class Util {

    //TODO: this method should go into graphql-java
    public static ObjectTypeDefinition getQueryType(TypeDefinitionRegistry typeDefinitionRegistry) {
        if (typeDefinitionRegistry.schemaDefinition().isPresent()) {
            List<OperationTypeDefinition> operationTypeDefinitions = typeDefinitionRegistry.schemaDefinition().get().getOperationTypeDefinitions();
            OperationTypeDefinition queryOp = operationTypeDefinitions.stream().filter(op -> "query".equals(op.getName())).findFirst().get();
            TypeName typeName = queryOp.getTypeName();
            return (ObjectTypeDefinition) typeDefinitionRegistry.getType(typeName).get();
        } else {
            return (ObjectTypeDefinition) typeDefinitionRegistry.getType("Query").get();
        }
    }

}
