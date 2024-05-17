package graphql.nadel.tests.next

import graphql.Assert.assertFalse
import graphql.ExecutionResult
import graphql.GraphQL
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import graphql.execution.SimpleDataFetcherExceptionHandler
import graphql.execution.instrumentation.Instrumentation
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.incremental.IncrementalExecutionResult
import graphql.language.AstPrinter
import graphql.nadel.Nadel
import graphql.nadel.NadelExecutionHints
import graphql.nadel.NadelExecutionInput
import graphql.nadel.NadelSchemas
import graphql.nadel.ServiceExecution
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.error.NadelGraphQLErrorException
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.tests.assertJsonEquals
import graphql.nadel.tests.compareJson
import graphql.nadel.tests.jsonObjectMapper
import graphql.nadel.tests.withPrettierPrinter
import graphql.nadel.validation.NadelSchemaValidation
import graphql.parser.Parser
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.test.runTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode
import java.util.concurrent.CompletableFuture
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

abstract class NadelIntegrationTest(
    val operationName: String? = null,
    @Language("GraphQL")
    val query: String,
    val variables: JsonMap = emptyMap(),
    val userContext: Any? = null,
    val services: List<Service>,
) {
    private val executionCapture = TestExecutionCapture()

    open val name: String get() = this::class.asTestName()

    @Test
    fun execute() = runTest(timeout = 10.minutes) {
        // Given
        val testData = getTestSnapshot()

        val nadel = makeNadel()
            .build()

        val executionInput = makeExecutionInput().build()

        // When
        val result = nadel.execute(executionInput).await()

        val incrementalResults = if (result is IncrementalExecutionResult) {
            result.incrementalItemPublisher
                .asFlow()
                .toList()
        } else {
            null
        }

        // Then
        assert(result, incrementalResults)
        assertNadelResult(result, incrementalResults, testData)
        assertServiceCalls(testData)
    }

    /**
     * Executes the test without @defer to see if the result will be the same as combining
     * the expected partial results.
     */
    @Test
    fun executeNoDefer() = runTest {
        val testSnapshot = getTestSnapshot()

        assumeTrue(testSnapshot.result.delayedResults.isNotEmpty())

        val nadel = makeNadel()
            .build()

        val executionInput = makeExecutionInput().build()

        // Given
        val combinedDeferResultMap = combineExecutionResults(
            result = testSnapshot.result.result,
            incrementalResults = testSnapshot.result.delayedResults,
        )

        // When
        val noDeferResult = nadel
            .execute(
                executionInput.copy(
                    query = stripDefer(executionInput.query),
                ),
            )
            .await()

        // Then
        assertTrue(noDeferResult !is IncrementalExecutionResult)

        // Compare data strictly, must equal 1-1
        val noDeferResultMap = noDeferResult.toSpecification()
        assertJsonEquals(
            expected = mapOf("data" to noDeferResultMap["data"]),
            actual = mapOf("data" to combinedDeferResultMap["data"]),
            mode = JSONCompareMode.STRICT,
        )
        // Compare rest of data, these can be more lenient
        // Maybe this won't hold out longer term, but e.g. it's ok for the deferred errors to add a path
        assertJsonEquals(
            expected = mapOf(
                "errors" to noDeferResultMap["errors"],
                "extensions" to noDeferResultMap["extensions"],
            ),
            actual = mapOf(
                "errors" to combinedDeferResultMap["errors"],
                "extensions" to combinedDeferResultMap["extensions"],
            ),
            mode = JSONCompareMode.LENIENT,
        )

        assertTrue(noDeferResultMap.keys == combinedDeferResultMap.keys)
    }

    suspend fun capture(): TestExecutionCapture {
        val nadel = makeNadel()
            .build()

        val result = nadel
            .execute(makeExecutionInput().build())
            .let {
                executionCapture.capture(it.await())
            }

        if (result is IncrementalExecutionResult) {
            // Drain incremental results before continuing
            result.incrementalItemPublisher
                .asFlow()
                .collect()
        }

        return executionCapture
    }

    open fun makeExecutionInput(): NadelExecutionInput.Builder {
        return NadelExecutionInput.Builder()
            .query(query)
            .variables(variables)
            .operationName(operationName)
            .context(userContext)
            .executionHints(makeExecutionHints().build())
    }

    open fun makeExecutionHints(): NadelExecutionHints.Builder {
        return NadelExecutionHints.Builder()
    }

    open fun makeNadel(): Nadel.Builder {
        val schemas = makeNadelSchemas().build()
        val schemaErrors = NadelSchemaValidation(schemas).validate()
        assertTrue(schemaErrors.isEmpty())

        return Nadel.newNadel()
            .schemas(schemas)
            .instrumentation(makeInstrumentation())
    }

    open fun makeNadelSchemas(): NadelSchemas.Builder {
        require(services.asSequence().map { it.name }.toSet().size == services.size)

        val overallSchemas = services.associate { it.name to it.overallSchema }
        val underlyingSchemas = services.associate { it.name to it.underlyingSchema }
        val serviceExecutions = services.associate { service ->
            val serviceName = service.name
            serviceName to makeServiceExecution(service)
        }

        return NadelSchemas.newNadelSchemas()
            .overallSchemas(overallSchemas)
            .underlyingSchemas(underlyingSchemas)
            .serviceExecutionFactory { service ->
                serviceExecutions[service]!!
            }
    }

    open fun makeServiceExecution(service: Service): ServiceExecution {
        return GraphQLServiceExecution(
            serviceName = service.name,
            graphQL = makeServiceGraphQL(service).build(),
        )
    }

    open fun makeServiceGraphQL(service: Service): GraphQL.Builder {
        return GraphQL
            .newGraphQL(
                SchemaGenerator()
                    .makeExecutableSchema(
                        SchemaParser().parse(service.underlyingSchema),
                        RuntimeWiring.newRuntimeWiring()
                            .also(service.runtimeWiring)
                            .build(),
                    ),
            )
            .defaultDataFetcherExceptionHandler(
                object : DataFetcherExceptionHandler {
                    private val defaultImpl = SimpleDataFetcherExceptionHandler()

                    override fun handleException(
                        handlerParameters: DataFetcherExceptionHandlerParameters,
                    ): CompletableFuture<DataFetcherExceptionHandlerResult> {
                        val exception = handlerParameters.exception

                        return if (exception is NadelGraphQLErrorException) {
                            CompletableFuture.completedFuture(
                                DataFetcherExceptionHandlerResult.newResult()
                                    .error(exception)
                                    .build(),
                            )
                        } else {
                            defaultImpl.handleException(handlerParameters)
                        }
                    }
                },
            )
            .instrumentation(
                object : Instrumentation {
                    override fun instrumentExecutionResult(
                        executionResult: ExecutionResult,
                        parameters: InstrumentationExecutionParameters,
                        state: InstrumentationState?,
                    ): CompletableFuture<ExecutionResult> {
                        return CompletableFuture.completedFuture(
                            executionCapture.capture(
                                service = service.name,
                                query = parameters.query,
                                variables = parameters.variables,
                                result = executionResult,
                            ),
                        )
                    }
                },
            )
    }

    open fun makeInstrumentation(): NadelInstrumentation {
        return object : NadelInstrumentation {
        }
    }

    /**
     * Default backing value for [getTestSnapshot].
     */
    private val _testSnapshot = lazy {
        try {
            Class.forName(this::class.qualifiedName + "Snapshot")
                .getDeclaredConstructor()
                .newInstance() as TestSnapshot
        } catch (e: ClassNotFoundException) {
            throw ClassNotFoundException("Run UpdateTestSnapshots to write test snapshots", e)
        }
    }

    open fun getTestSnapshot(): TestSnapshot {
        return _testSnapshot.value
    }

    open fun assert(result: ExecutionResult, incrementalResults: List<DelayedIncrementalPartialResult>?) {
    }

    private fun assertServiceCalls(testSnapshot: TestSnapshot) {
        fun getCanonicalQuery(query: String): String {
            return AstPrinter.printAstCompact(Parser().parseDocument(query))
        }

        fun isDelayedResultsEqual(
            expectedCall: ExpectedServiceCall,
            actualCall: TestExecutionCapture.Call,
        ): Boolean {
            val (unmatchedExpectedDelayedResults, unmatchedActualDelayedResults) = getUnmatchedElements(
                expected = expectedCall.delayedResults,
                actual = actualCall.delayedResults
            ) { expectedDelayedResult, actualDelayedResult ->
                // Note: we compare hasNext further down in the function
                compareJson(
                    expectedDelayedResult - "hasNext",
                    actualDelayedResult.toSpecification() - "hasNext",
                ).passed()
            }

            return unmatchedExpectedDelayedResults.isEmpty() && unmatchedActualDelayedResults.isEmpty()
        }

        val (unmatchedExpectedCalls, unmatchedActualCalls) = getUnmatchedElements(
            expected = testSnapshot.calls,
            actual = executionCapture.calls
        ) { expectedCall, actualCall ->
            val actualQuery = getCanonicalQuery(actualCall.query)
            val actualVariables = actualCall.variables
            val actualResult = actualCall.result

            actualCall.service == expectedCall.service
                && expectedCall.delayedResults.size == actualCall.delayedResults.size
                && getCanonicalQuery(expectedCall.query) == actualQuery
                && compareJson(expected = expectedCall.variables, actual = actualVariables).passed()
                && compareJson(expected = expectedCall.result, actual = actualResult).passed()
                && isDelayedResultsEqual(expectedCall, actualCall)
        }

        // This will fail if there are any unmatched calls e.g.
        // unmatched because the number of calls was different
        // unmatched because the contents of the calls were different
        assertTrue(unmatchedExpectedCalls.isEmpty() && unmatchedActualCalls.isEmpty())

        // Make sure the hasNext is correct
        executionCapture.calls
            .forEach { actualCall ->
                val delayedResults = actualCall.delayedResults
                if (delayedResults.isNotEmpty()) {
                    assertTrue(delayedResults.dropLast(n = 1).all { it.hasNext() } && !delayedResults.last().hasNext())
                }
            }
    }

    private fun assertNadelResult(
        result: ExecutionResult,
        incrementalResults: List<DelayedIncrementalPartialResult>?,
        testSnapshot: TestSnapshot,
    ) {
        val combinedResult = jsonObjectMapper
            .withPrettierPrinter()
            .writeValueAsString(
                combineExecutionResults(
                    result = result.toSpecification(),
                    incrementalResults = incrementalResults
                        ?.map(DelayedIncrementalPartialResult::toSpecification)
                        ?: emptyList(),
                ),
            )
            .replaceIndent(' '.toString().repeat(4))

        println("Combined overall result was\n$combinedResult")

        assertJsonEquals(
            /* expectedStr = */ testSnapshot.result.result,
            /* actualStr = */ result.toSpecification(),
            /* compareMode = */ JSONCompareMode.STRICT,
        )

        if (testSnapshot.result.delayedResults.isEmpty()) {
            if (result is IncrementalExecutionResult) {
                assertTrue(incrementalResults?.isEmpty() != false) // Can be [true, null] (i.e. empty or non-existent)
                assertFalse(result.hasNext())
            }
        } else {
            // Note: there exists a IncrementalExecutionResult.getIncremental but that is part of the initial result
            assertTrue(result is IncrementalExecutionResult)

            // Fuck why delayed & incremental?? Shouldn't incremental == delayed? Why is there an optional synchronous incremental??
            val actualDelayedResponses = incrementalResults!!

            // Should only have one element that says hasNext=false, and it should be the last one
            assertTrue(actualDelayedResponses.dropLast(n = 1).all { it.hasNext() })
            assertFalse(actualDelayedResponses.last().hasNext())

            val (
                unmatchedExpectedDelayedResponses,
                unmatchedActualDelayedResponses,
            ) = getUnmatchedElements(
                expected = testSnapshot.result.delayedResults,
                actual = actualDelayedResponses.map(DelayedIncrementalPartialResult::toSpecification),
            ) { expectedResponse, actualResponse ->
                compareJson(
                    expectedResponse - "hasNext",
                    actualResponse - "hasNext",
                    JSONCompareMode.STRICT
                ).passed()
            }

            // This will fail if there are any unmatched responses e.g.
            // unmatched because the number of responses was different
            // unmatched because the contents of the responses were different
            assertTrue(unmatchedExpectedDelayedResponses.isEmpty() && unmatchedActualDelayedResponses.isEmpty())
        }
    }

    private fun <E, A> getUnmatchedElements(
        expected: List<E>,
        actual: List<A>,
        test: (E, A) -> Boolean,
    ): Pair<List<E>, List<A>> {
        val unmatchedExpected = expected.toMutableList()
        val unmatchedActual = actual.toMutableList()

        unmatchedExpected.forEachElementInIterator { unmatchedExpectedIterator, expectedElement ->
            val actualMatchIndex = unmatchedActual
                .indexOfFirst { actualElement ->
                    test(expectedElement, actualElement)
                }

            if (actualMatchIndex >= 0) {
                unmatchedExpectedIterator.remove()
                unmatchedActual.removeAt(actualMatchIndex)
            }
        }

        return unmatchedExpected to unmatchedActual
    }

    data class Service(
        val name: String,
        @Language("GraphQL")
        val overallSchema: String,
        @Language("GraphQL")
        val underlyingSchema: String = makeUnderlyingSchema(overallSchema),
        val runtimeWiring: (RuntimeWiring.Builder) -> Unit,
    )

    companion object {
        @JvmStatic
        protected val source = "\$source"
        protected val argument = "\$argument"
    }
}
