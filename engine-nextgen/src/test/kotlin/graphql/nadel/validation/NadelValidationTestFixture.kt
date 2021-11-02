package graphql.nadel.validation

import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionEngine
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import graphql.schema.idl.errors.SchemaProblem

data class NadelValidationTestFixture(
    val overallSchema: Map<String, String>,
    val underlyingSchema: Map<String, String>,
) {
    fun toNadel(): Nadel {
        return Nadel.newNadel()
            .engineFactory {
                NadelExecutionEngine { _, _, _, _ ->
                    error("no-op")
                }
            }
            .dsl(overallSchema)
            .serviceExecutionFactory(object : ServiceExecutionFactory {
                override fun getServiceExecution(serviceName: String?): ServiceExecution {
                    return ServiceExecution {
                        error("no-op")
                    }
                }

                private val schemaParser = SchemaParser()

                override fun getUnderlyingTypeDefinitions(serviceName: String): TypeDefinitionRegistry {
                    try {
                        val schemaText = underlyingSchema[serviceName]
                            ?: error("Unable to find underlying schema for service $serviceName")
                        return schemaParser.parse(schemaText)
                    } catch (e: SchemaProblem) {
                        throw RuntimeException("Unable to parse underlying schema for $serviceName", e)
                    }
                }
            })
            .build()
    }
}
