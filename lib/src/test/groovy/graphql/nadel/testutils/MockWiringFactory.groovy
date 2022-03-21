package graphql.nadel.testutils

import com.google.common.collect.ImmutableMap
import graphql.TypeResolutionEnvironment
import graphql.scalars.ExtendedScalars
import graphql.schema.DataFetcher
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLScalarType
import graphql.schema.PropertyDataFetcher
import graphql.schema.TypeResolver
import graphql.schema.idl.FieldWiringEnvironment
import graphql.schema.idl.InterfaceWiringEnvironment
import graphql.schema.idl.ScalarInfo
import graphql.schema.idl.ScalarWiringEnvironment
import graphql.schema.idl.UnionWiringEnvironment
import graphql.schema.idl.WiringFactory

class MockedWiringFactory implements WiringFactory {

    private static final ImmutableMap<String, GraphQLScalarType> SCALARS = ImmutableMap.of(
            ExtendedScalars.Json.getName(), ExtendedScalars.Json
    )

    @Override
    boolean providesScalar(ScalarWiringEnvironment env) {
        String scalarName = env.getScalarTypeDefinition().getName()
        if (SCALARS.containsKey(scalarName)) {
            return true
        }
        return !(ScalarInfo.isGraphqlSpecifiedScalar(scalarName))
    }

    @Override
    GraphQLScalarType getScalar(ScalarWiringEnvironment env) {
        String scalarName = env.getScalarTypeDefinition().getName()
        GraphQLScalarType scalarType = SCALARS.get(scalarName)
        if (scalarType != null) {
            return scalarType
        }
        return null
    }

    @Override
    boolean providesTypeResolver(InterfaceWiringEnvironment environment) {
        return true
    }

    @Override
    TypeResolver getTypeResolver(InterfaceWiringEnvironment environment) {
        new TypeResolver() {
            @Override
            GraphQLObjectType getType(TypeResolutionEnvironment env) {
                throw new UnsupportedOperationException("Not implemented")
            }
        }
    }

    @Override
    boolean providesTypeResolver(UnionWiringEnvironment environment) {
        return true
    }

    @Override
    TypeResolver getTypeResolver(UnionWiringEnvironment environment) {
        new TypeResolver() {
            @Override
            GraphQLObjectType getType(TypeResolutionEnvironment env) {
                throw new UnsupportedOperationException("Not implemented")
            }
        }
    }

    @Override
    boolean providesDataFetcher(FieldWiringEnvironment environment) {
        return true
    }

    @Override
    DataFetcher getDataFetcher(FieldWiringEnvironment environment) {
        return new PropertyDataFetcher(environment.getFieldDefinition().getName())
    }
}
