package graphql.nadel.schema

import graphql.Assert.assertShouldNeverHappen
import graphql.scalars.ExtendedScalars
import graphql.scalars.alias.AliasedScalar
import graphql.schema.DataFetcher
import graphql.schema.GraphQLScalarType
import graphql.schema.TypeResolver
import graphql.schema.idl.FieldWiringEnvironment
import graphql.schema.idl.InterfaceWiringEnvironment
import graphql.schema.idl.ScalarInfo
import graphql.schema.idl.ScalarWiringEnvironment
import graphql.schema.idl.UnionWiringEnvironment
import graphql.schema.idl.WiringFactory

/**
 * This wiring factory is designed to be NEVER called and will assert if it ever is. Nadel
 * uses this for the overall schema and also in part for the underlying schema by default.
 */
open class NeverWiringFactory : WiringFactory {
    override fun providesScalar(environment: ScalarWiringEnvironment): Boolean {
        val scalarName = environment.scalarTypeDefinition.name
        return !ScalarInfo.isGraphqlSpecifiedScalar(scalarName)
    }

    override fun getScalar(environment: ScalarWiringEnvironment): GraphQLScalarType? {
        return when (val scalarName = environment.scalarTypeDefinition.name) {
            ExtendedScalars.Json.name -> {
                ExtendedScalars.Json
            }
            else -> {
                AliasedScalar.Builder()
                    .name(scalarName)
                    .aliasedScalar(ExtendedScalars.Json)
                    .build()
            }
        }
    }

    override fun providesTypeResolver(environment: InterfaceWiringEnvironment): Boolean {
        return true
    }

    override fun getTypeResolver(environment: InterfaceWiringEnvironment): TypeResolver {
        return TypeResolver {
            assertShouldNeverHappen("This interface type resolver should NEVER be called from Nadel")
        }
    }

    override fun providesTypeResolver(environment: UnionWiringEnvironment): Boolean {
        return true
    }

    override fun getTypeResolver(environment: UnionWiringEnvironment): TypeResolver {
        return TypeResolver {
            assertShouldNeverHappen("This union type resolver should NEVER be called from Nadel")
        }
    }

    override fun providesDataFetcher(environment: FieldWiringEnvironment): Boolean {
        return true
    }

    override fun getDataFetcher(environment: FieldWiringEnvironment): DataFetcher<*> {
        return DataFetcher {
            assertShouldNeverHappen<Any?>("This data fetcher should NEVER be called from Nadel")
        }
    }

    override fun getDefaultDataFetcher(environment: FieldWiringEnvironment): DataFetcher<*> {
        return DataFetcher {
            assertShouldNeverHappen<Any?>("This data fetcher should NEVER be called from Nadel")
        }
    }
}
