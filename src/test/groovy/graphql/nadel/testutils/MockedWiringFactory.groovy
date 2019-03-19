package graphql.nadel.testutils

import graphql.TypeResolutionEnvironment
import graphql.schema.DataFetcher
import graphql.schema.GraphQLObjectType
import graphql.schema.PropertyDataFetcher
import graphql.schema.TypeResolver
import graphql.schema.idl.FieldWiringEnvironment
import graphql.schema.idl.InterfaceWiringEnvironment
import graphql.schema.idl.UnionWiringEnvironment
import graphql.schema.idl.WiringFactory

class MockedWiringFactory implements WiringFactory {

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
