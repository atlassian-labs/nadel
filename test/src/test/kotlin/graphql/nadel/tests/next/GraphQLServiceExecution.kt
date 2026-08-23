package graphql.nadel.tests.next

import graphql.ExecutionInput
import graphql.ExperimentalApi
import graphql.GraphQL
import graphql.incremental.IncrementalExecutionResult
import graphql.language.AstPrinter
import graphql.nadel.NadelIncrementalServiceExecutionResult
import graphql.nadel.NadelServiceExecutionResultImpl
import graphql.nadel.ServiceExecution
import graphql.nadel.ServiceExecutionParameters
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.tests.jsonObjectMapper
import graphql.nadel.tests.withPrettierPrinter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.asPublisher
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
                onServiceResponse(input, result?.toSpecification(), exception)
            }
            .thenApply {
                if (it is IncrementalExecutionResult) {
                    val spec = it.toSpecification()

                    @Suppress("UNCHECKED_CAST")
                    NadelIncrementalServiceExecutionResult(
                        data = spec["data"] as MutableMap<String, Any?>? ?: mutableMapOf(),
                        errors = spec["errors"] as MutableList<MutableMap<String, Any?>?>? ?: mutableListOf(),
                        extensions = spec["extensions"] as MutableMap<String, Any?>? ?: mutableMapOf(),
                        incremental = it.incremental,
                        incrementalItemPublisher = it.incrementalItemPublisher
                            .asFlow()
                            .catch { e ->
                                onServiceResponse(input, success = null, e)
                                throw e
                            }
                            .onEach { result ->
                                onServiceResponse(input, result?.toSpecification(), failure = null)
                            }
                            .asPublisher(),
                        hasNext = it.hasNext(),
                    )
                } else {
                    val spec = it.toSpecification()

                    @Suppress("UNCHECKED_CAST")
                    NadelServiceExecutionResultImpl(
                        data = spec["data"] as MutableMap<String, Any?>? ?: mutableMapOf(),
                        errors = spec["errors"] as MutableList<MutableMap<String, Any?>?>? ?: mutableListOf(),
                        extensions = spec["extensions"] as MutableMap<String, Any?>? ?: mutableMapOf(),
                    )
                }
            }
    }

    private val fourSpaces = ' '.toString().repeat(4)

    private fun onServiceResponse(
        input: ExecutionInput,
        success: JsonMap?,
        failure: Throwable?,
    ) {
        val query = input.query

        val variables = jsonObjectMapper
            .withPrettierPrinter()
            .writeValueAsString(input.variables)

        val response = failure?.stackTraceToString()
            ?: jsonObjectMapper
                .withPrettierPrinter()
                .writeValueAsString(success!!)

        // Deliberately did not use trimIndent etc. because of formatting
        val logMessage = """
The $serviceName service was invoked with operation
${query.replaceIndent(fourSpaces)}
and variables
${variables.replaceIndent(fourSpaces)}
and responded with
${response.replaceIndent(fourSpaces)}
"""

        // println(logMessage)
    }
}
