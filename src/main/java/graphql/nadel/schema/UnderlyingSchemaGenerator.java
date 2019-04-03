package graphql.nadel.schema;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import graphql.schema.idl.InterfaceWiringEnvironment;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.UnionWiringEnvironment;
import graphql.schema.idl.WiringFactory;

import java.util.Map;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertTrue;

public class UnderlyingSchemaGenerator {

    public GraphQLSchema buildUnderlyingSchema(TypeDefinitionRegistry underlyingTypeDefinitions) {
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .wiringFactory(underScoreTypeWiringFactory())
                .build();
        return schemaGenerator.makeExecutableSchema(underlyingTypeDefinitions, runtimeWiring);
    }

    private WiringFactory underScoreTypeWiringFactory() {
        // uses a Never wiring factory except for type resolving of unions  and interfaces
        return new NeverWiringFactory() {
            @Override
            public boolean providesTypeResolver(InterfaceWiringEnvironment environment) {
                return true;
            }

            @Override
            public TypeResolver getTypeResolver(InterfaceWiringEnvironment environment) {
                return underScoreTypeNameResolver();
            }

            @Override
            public boolean providesTypeResolver(UnionWiringEnvironment environment) {
                return true;
            }

            @Override
            public TypeResolver getTypeResolver(UnionWiringEnvironment environment) {
                return underScoreTypeNameResolver();
            }

            @SuppressWarnings({"unchecked", "ConstantConditions"})
            private TypeResolver underScoreTypeNameResolver() {
                return env -> {
                    Object source = env.getObject();
                    assertTrue(source instanceof Map, "The Nadel result object MUST be a Map");

                    Map<String, Object> sourceMap = (Map<String, Object>) source;
                    assertTrue(sourceMap.containsKey("__typename"), "The Nadel result object for interfaces and unions MUST have __typename in them");

                    Object typeName = sourceMap.get("__typename");
                    assertNotNull(typeName, "The Nadel result object for interfaces and unions MUST have __typename with a non null value in them in them");

                    GraphQLObjectType objectType = env.getSchema().getObjectType(typeName.toString());
                    assertNotNull(objectType, "There must be an underlying object type called '%s'", typeName);
                    return objectType;
                };
            }
        };
    }
}
