package graphql.nadel.validation

import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.execution.instrumentation.InstrumentationState
import graphql.language.Document
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionEngine
import graphql.nadel.NadelExecutionParams
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionFactory
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import graphql.schema.idl.errors.SchemaProblem
import java.util.concurrent.CompletableFuture

data class NadelValidationTestFixture(
    val overallSchema: Map<String, String>,
    val underlyingSchema: Map<String, String>,
) {
    fun toNadel(): Nadel {
        return Nadel.newNadel()
            .engineFactory {
                object : NadelExecutionEngine {
                    override fun execute(
                        executionInput: ExecutionInput,
                        queryDocument: Document,
                        instrumentationState: InstrumentationState?,
                        nadelExecutionParams: NadelExecutionParams,
                    ): CompletableFuture<ExecutionResult> {
                        error("no-op")
                    }
                }
            }
            .dsl(overallSchema)
            .serviceExecutionFactory(object : ServiceExecutionFactory {
                override fun getServiceExecution(serviceName: String): ServiceExecution {
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
