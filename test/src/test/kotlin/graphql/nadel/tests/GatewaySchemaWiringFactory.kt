package graphql.nadel.tests

import graphql.Scalars
import graphql.nadel.schema.NeverWiringFactory
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLScalarType
import graphql.schema.idl.ScalarInfo
import graphql.schema.idl.ScalarWiringEnvironment
import java.util.concurrent.ConcurrentHashMap

/**
 * Custom scalars supported by the AGG. We build tests here to ensure they work
 * and to catch issues with them early on in development.
 */
class GatewaySchemaWiringFactory : NeverWiringFactory() {
    private val passThruScalars: MutableMap<String, GraphQLScalarType> = ConcurrentHashMap()

    override fun providesScalar(env: ScalarWiringEnvironment): Boolean {
        val scalarName = env.scalarTypeDefinition.name
        return if (defaultScalars.containsKey(scalarName)) {
            true
        } else !ScalarInfo.isGraphqlSpecifiedScalar(scalarName)
    }

    override fun getScalar(env: ScalarWiringEnvironment): GraphQLScalarType {
        val scalarName = env.scalarTypeDefinition.name
        val scalarType = defaultScalars[scalarName]
        return scalarType ?: passThruScalars.computeIfAbsent(scalarName) {
            passThruScalar(env)
        }
    }

    private fun passThruScalar(env: ScalarWiringEnvironment): GraphQLScalarType {
        val scalarTypeDefinition = env.scalarTypeDefinition
        val scalarName = scalarTypeDefinition.name
        val scalarDescription = if (scalarTypeDefinition.description == null) {
            scalarName
        } else {
            scalarTypeDefinition.description.content
        }

        return GraphQLScalarType.newScalar().name(scalarName)
            .definition(scalarTypeDefinition)
            .description(scalarDescription)
            .coercing(ExtendedScalars.Json.coercing)
            .build()
    }

    companion object {
        private val urlScalar = GraphQLScalarType.newScalar()
            .name("URL")
            .description("A URL Scalar type")
            .coercing(Scalars.GraphQLString.coercing)
            .build()

        private val dateTimeScalar = GraphQLScalarType.newScalar()
            .name("DateTime")
            .description("DateTime type")
            .coercing(Scalars.GraphQLString.coercing)
            .build()

//        private val jsonScalar = GraphQLScalarType.newScalar()
//            .name("JSON")
//            .description("JSON Scalar type")
//            .coercing(ExtendedScalars.Json.coercing)
//            .build()

        private val defaultScalars = mapOf(
            urlScalar.name to urlScalar,
            ExtendedScalars.Json.name to ExtendedScalars.Json,
            ExtendedScalars.GraphQLLong.name to ExtendedScalars.GraphQLLong,
            dateTimeScalar.name to dateTimeScalar,
//            jsonScalar.name to jsonScalar,
        )
    }
}
