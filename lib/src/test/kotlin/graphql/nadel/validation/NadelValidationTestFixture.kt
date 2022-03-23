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
            .overallSchemas(overallSchema)
            .underlyingSchemas(underlyingSchema)
            .serviceExecutionFactory(object : ServiceExecutionFactory {
                override fun getServiceExecution(serviceName: String): ServiceExecution {
                    return ServiceExecution {
                        error("no-op")
                    }
                }
            })
            .build()
    }
}
