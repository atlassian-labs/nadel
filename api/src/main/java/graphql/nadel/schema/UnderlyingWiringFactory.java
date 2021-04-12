package graphql.nadel.schema;

import graphql.Assert;
import graphql.Internal;
import graphql.nadel.engine.NadelContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetcherFactory;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.TypeResolver;
import graphql.schema.idl.FieldWiringEnvironment;
import graphql.schema.idl.InterfaceWiringEnvironment;
import graphql.schema.idl.ScalarWiringEnvironment;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import graphql.schema.idl.UnionWiringEnvironment;
import graphql.schema.idl.WiringFactory;

import java.util.Map;

/**
 * This underlying wiring factory has special type resolver support that is needed by Nadel.
 */
@Internal
public class UnderlyingWiringFactory implements WiringFactory {

    private final WiringFactory delegateWiringFactory;

    public UnderlyingWiringFactory(WiringFactory delegateWiringFactory) {
        this.delegateWiringFactory = delegateWiringFactory;
    }

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
            NadelContext nadelContext = env.getContext();
            String underscoreTypeNameAlias = nadelContext.getUnderscoreTypeNameAlias();

            Object source = env.getObject();
            Assert.assertTrue(source instanceof Map, () -> "The Nadel result object MUST be a Map");

            Map<String, Object> sourceMap = (Map<String, Object>) source;
            Assert.assertTrue(sourceMap.containsKey(underscoreTypeNameAlias), () -> "The Nadel result object for interfaces and unions MUST have an aliased __typename in them");

            Object typeName = sourceMap.get(underscoreTypeNameAlias);
            Assert.assertNotNull(typeName, () -> "The Nadel result object for interfaces and unions MUST have an aliased__typename with a non null value in them");

            GraphQLObjectType objectType = env.getSchema().getObjectType(typeName.toString());
            Assert.assertNotNull(objectType, () -> String.format("There must be an underlying graphql object type called '%s'", typeName));
            return objectType;
        };
    }

    @Override
    public boolean providesScalar(ScalarWiringEnvironment environment) {
        return delegateWiringFactory.providesScalar(environment);
    }

    @Override
    public GraphQLScalarType getScalar(ScalarWiringEnvironment environment) {
        return delegateWiringFactory.getScalar(environment);
    }

    @Override
    public boolean providesDataFetcherFactory(FieldWiringEnvironment environment) {
        return delegateWiringFactory.providesDataFetcherFactory(environment);
    }

    @Override
    public <T> DataFetcherFactory<T> getDataFetcherFactory(FieldWiringEnvironment environment) {
        return delegateWiringFactory.getDataFetcherFactory(environment);
    }

    @Override
    public boolean providesSchemaDirectiveWiring(SchemaDirectiveWiringEnvironment environment) {
        return delegateWiringFactory.providesSchemaDirectiveWiring(environment);
    }

    @Override
    public SchemaDirectiveWiring getSchemaDirectiveWiring(SchemaDirectiveWiringEnvironment environment) {
        return delegateWiringFactory.getSchemaDirectiveWiring(environment);
    }

    @Override
    public boolean providesDataFetcher(FieldWiringEnvironment environment) {
        return delegateWiringFactory.providesDataFetcher(environment);
    }

    @Override
    public DataFetcher getDataFetcher(FieldWiringEnvironment environment) {
        return delegateWiringFactory.getDataFetcher(environment);
    }

    @Override
    public DataFetcher getDefaultDataFetcher(FieldWiringEnvironment environment) {
        return delegateWiringFactory.getDefaultDataFetcher(environment);
    }
}
