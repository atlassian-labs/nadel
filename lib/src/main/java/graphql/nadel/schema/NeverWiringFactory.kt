package graphql.nadel.schema

import graphql.Assert.assertShouldNeverHappen
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.Value
import graphql.scalars.ExtendedScalars
import graphql.schema.Coercing
import graphql.schema.DataFetcher
import graphql.schema.DataFetcherFactory
import graphql.schema.GraphQLCodeRegistry
import graphql.schema.GraphQLScalarType
import graphql.schema.TypeResolver
import graphql.schema.idl.FieldWiringEnvironment
import graphql.schema.idl.InterfaceWiringEnvironment
import graphql.schema.idl.ScalarInfo
import graphql.schema.idl.ScalarWiringEnvironment
import graphql.schema.idl.UnionWiringEnvironment
import graphql.schema.idl.WiringFactory
import java.util.Locale

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
            ExtendedScalars.GraphQLLong.name -> {
                ExtendedScalars.GraphQLLong
            }
            else -> {
                GraphQLScalarType.newScalar()
                    .name(scalarName)
                    .definition(environment.scalarTypeDefinition)
                    .coercing(object : Coercing<Any?, Any?> {
                        override fun serialize(
                            dataFetcherResult: Any,
                            graphQLContext: GraphQLContext,
                            locale: Locale,
                        ): Any? {
                            return assertShouldNeverHappen(
                                "This %s scalar coercing should NEVER be called from Nadel",
                                scalarName
                            )
                        }

                        override fun parseValue(
                            input: Any,
                            graphQLContext: GraphQLContext,
                            locale: Locale,
                        ): Any? {
                            return assertShouldNeverHappen(
                                "This %s scalar coercing should NEVER be called from Nadel",
                                scalarName
                            )
                        }

                        override fun parseLiteral(
                            input: Value<*>,
                            variables: CoercedVariables,
                            graphQLContext: GraphQLContext,
                            locale: Locale,
                        ): Any? {
                            return assertShouldNeverHappen(
                                "This %s scalar coercing should NEVER be called from Nadel",
                                scalarName
                            )
                        }
                    })
                    .build()
            }
        }
    }

    final override fun providesTypeResolver(environment: InterfaceWiringEnvironment): Boolean {
        return true
    }

    final override fun getTypeResolver(environment: InterfaceWiringEnvironment): TypeResolver {
        return NEVER_TR
    }

    final override fun providesTypeResolver(environment: UnionWiringEnvironment): Boolean {
        return true
    }

    final override fun getTypeResolver(environment: UnionWiringEnvironment): TypeResolver {
        return TypeResolver {
            assertShouldNeverHappen("This union type resolver should NEVER be called from Nadel")
        }
    }

    final override fun providesDataFetcher(environment: FieldWiringEnvironment): Boolean {
        return false
    }

    final override fun getDataFetcher(environment: FieldWiringEnvironment): DataFetcher<*> {
        return assertShouldNeverHappen("getDataFetcher should NEVER be called from Nadel - we returned false")
    }

    final override fun getDefaultDataFetcher(environment: FieldWiringEnvironment): DataFetcher<*>? {
        return null
    }


    companion object {
        val NEVER_TR =  TypeResolver {
            assertShouldNeverHappen("This interface type resolver should NEVER be called from Nadel")
        }
        private val NEVER_DF = DataFetcher {
            assertShouldNeverHappen<Any?>("This data fetcher should NEVER be called from Nadel")
        }
        private val NEVER_DFF = DataFetcherFactory {
            NEVER_DF
        }

        val NEVER_CODE_REGISTRY: GraphQLCodeRegistry = GraphQLCodeRegistry.newCodeRegistry()
            .defaultDataFetcher(NEVER_DFF).build()

    }
}
