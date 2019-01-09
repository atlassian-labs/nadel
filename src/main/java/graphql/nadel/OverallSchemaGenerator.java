package graphql.nadel;

import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.SDLDefinition;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.ArrayList;
import java.util.List;

import static graphql.language.ObjectTypeDefinition.newObjectTypeDefinition;

public class OverallSchemaGenerator {


    public GraphQLSchema buildOverallSchema(List<Service> services) {
        //TODO: This will not work for Unions and interfaces as they require TypeResolver
        // need to loose this requirement or add dummy versions
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring().build();
        return schemaGenerator.makeExecutableSchema(createTypeRegistry(services), runtimeWiring);

    }

    private TypeDefinitionRegistry createTypeRegistry(List<Service> services) {
        //TODO: this merging not completely correct for example schema definition nodes are not handled correctly
        List<FieldDefinition> queryFields = new ArrayList<>();
        TypeDefinitionRegistry overallRegistry = new TypeDefinitionRegistry();
        List<SDLDefinition> allDefinitions = new ArrayList<>();

        for (Service service : services) {
            DefinitionRegistry definitionRegistry = service.getDefinitionRegistry();
            ObjectTypeDefinition queryType = definitionRegistry.getQueryType();
            queryFields.addAll(queryType.getFieldDefinitions());
            definitionRegistry
                    .getDefinitions()
                    .stream()
                    .filter(sdlDefinition -> sdlDefinition != queryType)
                    .forEach(allDefinitions::add);
        }
        ObjectTypeDefinition queryType = newObjectTypeDefinition().name("Query").fieldDefinitions(queryFields).build();
        overallRegistry.add(queryType);
        allDefinitions.forEach(overallRegistry::add);
        return overallRegistry;
    }

}
