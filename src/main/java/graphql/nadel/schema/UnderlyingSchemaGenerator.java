package graphql.nadel.schema;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.WiringFactory;

public class UnderlyingSchemaGenerator {

    public GraphQLSchema buildUnderlyingSchema(TypeDefinitionRegistry underlyingTypeDefinitions, WiringFactory wiringFactory) {
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .wiringFactory(new UnderlyingWiringFactory(wiringFactory))
                .build();
        return schemaGenerator.makeExecutableSchema(underlyingTypeDefinitions, runtimeWiring);
    }
}
