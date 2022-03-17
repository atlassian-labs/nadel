package graphql.nadel.schema

import graphql.Internal
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.TypeDefinitionRegistry
import graphql.schema.idl.WiringFactory
import graphql.schema.idl.errors.SchemaProblem

internal class UnderlyingSchemaGenerator {
    fun buildUnderlyingSchema(
        serviceName: String,
        underlyingTypeDefinitions: TypeDefinitionRegistry,
        wiringFactory: WiringFactory,
    ): GraphQLSchema {
        val schemaGenerator = SchemaGenerator()
        val runtimeWiring = RuntimeWiring.newRuntimeWiring()
            .wiringFactory(wiringFactory)
            .build()
        return try {
            schemaGenerator.makeExecutableSchema(underlyingTypeDefinitions, runtimeWiring)
        } catch (schemaProblem: SchemaProblem) {
            throw ServiceSchemaProblem(
                message = "There was a problem building the schema for '${serviceName}': ${schemaProblem.message}",
                serviceName = serviceName,
                cause = schemaProblem,
            )
        }
    }
}
