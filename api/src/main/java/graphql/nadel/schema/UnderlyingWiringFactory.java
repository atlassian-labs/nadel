package graphql.nadel.schema;

import graphql.Internal;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetcherFactory;
import graphql.schema.GraphQLScalarType;
import graphql.schema.TypeResolver;
import graphql.schema.idl.FieldWiringEnvironment;
import graphql.schema.idl.InterfaceWiringEnvironment;
import graphql.schema.idl.ScalarWiringEnvironment;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import graphql.schema.idl.UnionWiringEnvironment;
import graphql.schema.idl.WiringFactory;

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
        return delegateWiringFactory.getTypeResolver(environment);
    }

    @Override
    public boolean providesTypeResolver(UnionWiringEnvironment environment) {
        return delegateWiringFactory.providesTypeResolver(environment);
    }

    @Override
    public TypeResolver getTypeResolver(UnionWiringEnvironment environment) {
        return delegateWiringFactory.getTypeResolver(environment);
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
