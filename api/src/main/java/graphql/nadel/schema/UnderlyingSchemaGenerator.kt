package graphql.nadel.schema;

import graphql.Internal;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.WiringFactory;
import graphql.schema.idl.errors.SchemaProblem;

@Internal
public class UnderlyingSchemaGenerator {

    public GraphQLSchema buildUnderlyingSchema(String serviceName, TypeDefinitionRegistry underlyingTypeDefinitions, WiringFactory wiringFactory) throws ServiceSchemaProblem {
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .wiringFactory(new UnderlyingWiringFactory(wiringFactory))
                .build();
        try {
            return schemaGenerator.makeExecutableSchema(underlyingTypeDefinitions, runtimeWiring);
        } catch (SchemaProblem schemaProblem) {
            String message = String.format("There was a problem building the schema for '%s' : %s",
                    serviceName, schemaProblem.getMessage());
            throw new ServiceSchemaProblem(message, serviceName, schemaProblem);
        }
    }
}
