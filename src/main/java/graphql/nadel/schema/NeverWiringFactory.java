package graphql.nadel.schema;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLScalarType;
import graphql.schema.TypeResolver;
import graphql.schema.idl.FieldWiringEnvironment;
import graphql.schema.idl.InterfaceWiringEnvironment;
import graphql.schema.idl.ScalarInfo;
import graphql.schema.idl.ScalarWiringEnvironment;
import graphql.schema.idl.UnionWiringEnvironment;
import graphql.schema.idl.WiringFactory;

import static graphql.Assert.assertShouldNeverHappen;

/**
 * This wiring factory is designed to be NEVER called and will assert if it ever is.  Nadel
 * uses this for the overall schema and also in part for the underlying schema by default.
 */
public class NeverWiringFactory implements WiringFactory {
    @Override
    public boolean providesScalar(ScalarWiringEnvironment environment) {
        final String scalarName = environment.getScalarTypeDefinition().getName();
        return !(ScalarInfo.isStandardScalar(scalarName) || ScalarInfo.isGraphqlSpecifiedScalar(scalarName));
    }

    @Override
    public GraphQLScalarType getScalar(ScalarWiringEnvironment environment) {
        final String scalarName = environment.getScalarTypeDefinition().getName();
        return GraphQLScalarType.newScalar().name(scalarName)
                .definition(environment.getScalarTypeDefinition())
                .coercing(new Coercing() {
                    @Override
                    public Object serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        return assertShouldNeverHappen("This %s scalar coercing should NEVER be called from Nadel", scalarName);
                    }

                    @Override
                    public Object parseValue(Object input) throws CoercingParseValueException {
                        return assertShouldNeverHappen("This %s scalar coercing should NEVER be called from Nadel", scalarName);
                    }

                    @Override
                    public Object parseLiteral(Object input) throws CoercingParseLiteralException {
                        return assertShouldNeverHappen("This %s scalar coercing should NEVER be called from Nadel", scalarName);
                    }
                })
                .build();
    }

    @Override
    public boolean providesTypeResolver(InterfaceWiringEnvironment environment) {
        return true;
    }

    @Override
    public TypeResolver getTypeResolver(InterfaceWiringEnvironment environment) {
        return env -> assertShouldNeverHappen("This interface type resolver should NEVER be called from Nadel");
    }

    @Override
    public boolean providesTypeResolver(UnionWiringEnvironment environment) {
        return true;
    }

    @Override
    public TypeResolver getTypeResolver(UnionWiringEnvironment environment) {
        return env -> assertShouldNeverHappen("This union type resolver should NEVER be called from Nadel");
    }

    @Override
    public boolean providesDataFetcher(FieldWiringEnvironment environment) {
        return true;
    }

    @Override
    public DataFetcher getDataFetcher(FieldWiringEnvironment environment) {
        return env -> assertShouldNeverHappen("This data fetcher should NEVER be called from Nadel");
    }

    @Override
    public DataFetcher getDefaultDataFetcher(FieldWiringEnvironment environment) {
        return env -> assertShouldNeverHappen("This data fetcher should NEVER be called from Nadel");
    }
}
