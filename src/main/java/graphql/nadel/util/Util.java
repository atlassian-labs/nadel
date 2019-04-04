package graphql.nadel.util;

import graphql.Internal;
import graphql.language.Definition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.OperationTypeDefinition;
import graphql.language.SDLDefinition;
import graphql.language.TypeName;
import graphql.nadel.DefinitionRegistry;
import graphql.nadel.dsl.ServiceDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphQLUnionType;
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

    public static DefinitionRegistry buildServiceRegistry(ServiceDefinition serviceDefinition) {
        DefinitionRegistry definitionRegistry = new DefinitionRegistry();
        for (Definition definition : serviceDefinition.getTypeDefinitions()) {
            definitionRegistry.add((SDLDefinition) definition);
        }
        return definitionRegistry;
    }

    public static boolean isInterfaceOrUnionField(GraphQLOutputType fieldOutputType) {
        return GraphQLTypeUtil.unwrapAll(fieldOutputType) instanceof GraphQLInterfaceType || fieldOutputType instanceof GraphQLUnionType;
    }

}
