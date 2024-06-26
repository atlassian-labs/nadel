package graphql.nadel.tests.next

import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.ExperimentalApi
import graphql.GraphQL
import graphql.incremental.IncrementalExecutionResult
import graphql.language.AstPrinter
import graphql.nadel.NadelIncrementalServiceExecutionResult
import graphql.nadel.NadelServiceExecutionResultImpl
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionParameters
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.tests.jsonObjectMapper
import graphql.nadel.tests.withPrettierPrinter
import java.util.concurrent.CompletableFuture

class GraphQLServiceExecution(
    private val serviceName: String,
    private val graphQL: GraphQL,
) : ServiceExecution {
    override fun execute(serviceExecutionParameters: ServiceExecutionParameters): CompletableFuture<ServiceExecutionResult> {
        val input = ExecutionInput.newExecutionInput()
            .query(AstPrinter.printAst(serviceExecutionParameters.query))
            .variables(serviceExecutionParameters.variables)
            .graphQLContext(mapOf(ExperimentalApi.ENABLE_INCREMENTAL_SUPPORT to true))
            .build()

        return graphQL
            .executeAsync(input)
            .whenComplete { result, exception ->
                onServiceCall(input, result, exception)
            }
            .thenApply {
                if (it is IncrementalExecutionResult) {
                    val spec = it.toSpecification()

                    @Suppress("UNCHECKED_CAST")
                    NadelIncrementalServiceExecutionResult(
                        data = spec["data"] as MutableMap<String, Any?>? ?: mutableMapOf(),
                        errors = spec["errors"] as MutableList<MutableMap<String, Any?>>? ?: mutableListOf(),
                        extensions = spec["extensions"] as MutableMap<String, Any?>? ?: mutableMapOf(),
                        incremental = it.incremental,
                        incrementalItemPublisher = it.incrementalItemPublisher,
                        hasNext = it.hasNext(),
                    )
                } else {
                    val spec = it.toSpecification()

                    @Suppress("UNCHECKED_CAST")
                    NadelServiceExecutionResultImpl(
                        data = spec["data"] as MutableMap<String, Any?>? ?: mutableMapOf(),
                        errors = spec["errors"] as MutableList<MutableMap<String, Any?>>? ?: mutableListOf(),
                        extensions = spec["extensions"] as MutableMap<String, Any?>? ?: mutableMapOf(),
                    )
                }
            }
    }

    private fun onServiceCall(
        input: ExecutionInput,
        success: ExecutionResult?,
        failure: Throwable?,
    ) {
        val fourSpaces = ' '.toString().repeat(4)
        val query = input.query

        val variables = jsonObjectMapper
            .withPrettierPrinter()
            .writeValueAsString(input.variables)

        val response = failure?.stackTraceToString()
            ?: jsonObjectMapper
                .withPrettierPrinter()
                .writeValueAsString(success!!.toSpecification())

        // Deliberately did not use trimIndent etc. because of formatting
        val logMessage = """
The $serviceName service was invoked with operation
${query.replaceIndent(fourSpaces)}
and variables
${variables.replaceIndent(fourSpaces)}
and responded with
${response.replaceIndent(fourSpaces)}
"""

        println(logMessage)
    }
}
