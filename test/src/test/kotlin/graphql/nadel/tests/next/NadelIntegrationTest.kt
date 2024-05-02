package graphql.nadel.tests.next

import com.fasterxml.jackson.module.kotlin.readValue
import graphql.Assert.assertFalse
import graphql.ExecutionResult
import graphql.GraphQL
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
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.MutableJsonMap
import graphql.nadel.instrumentation.NadelInstrumentation
import graphql.nadel.tests.jsonObjectMapper
import graphql.nadel.tests.withPrettierPrinter
import graphql.parser.Parser
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.test.runTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompare
import org.skyscreamer.jsonassert.JSONCompareMode
import java.util.concurrent.CompletableFuture
import kotlin.test.assertTrue

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
    fun execute() = runTest {
        // Given
        val testData = getTestData()

        val nadel = makeNadel()
            .build()

        // When
        val result = nadel.execute(makeExecutionInput().build())

        // Then
        assert(result.toSpecification())
        assertNadelResult(result, testData)
        assertServiceCalls(testData)
    }

    suspend fun capture(): TestExecutionCapture {
        val nadel = makeNadel()
            .build()

        val result = nadel
            .execute(makeExecutionInput().build())
            .let {
                executionCapture.capture(it)
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
        return Nadel.newNadel()
            .schemas(makeNadelSchemas().build())
            .instrumentation(makeInstrumentation())
    }

    open fun makeNadelSchemas(): NadelSchemas.Builder {
        require(services.asSequence().map { it.name }.toSet().size == services.size)

        val overallSchemas = services.associate { it.name to it.overallSchema }
        val underlyingSchemas = services.associate { it.name to it.underlyingSchema }
        val serviceExecutions = services.associate {
            val serviceName = it.name
            serviceName to GraphQLServiceExecution(
                serviceName = serviceName,
                graphQL = makeServiceGraphQL(it).build(),
            )
        }

        return NadelSchemas.newNadelSchemas()
            .overallSchemas(overallSchemas)
            .underlyingSchemas(underlyingSchemas)
            .serviceExecutionFactory { service ->
                serviceExecutions[service]!!
            }
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

    open fun getTestData(): TestData {
        return Class.forName(this::class.qualifiedName + "Data")
            .getDeclaredConstructor()
            .newInstance() as TestData
    }

    open fun assert(result: JsonMap) {
    }

    private fun assertServiceCalls(testData: TestData) {
        // Unmatched calls, by the end of the function both should be empty if they're matched
        val unmatchedExpectedCalls = testData.calls.toMutableList()
        val unmatchedActualCalls = executionCapture.calls.toMutableList()

        unmatchedActualCalls.forEachElementInIterator { iterator, actualCall ->
            fun getCanonicalQuery(query: String): String {
                return AstPrinter.printAstCompact(Parser().parseDocument(query))
            }

            val actualQuery = getCanonicalQuery(actualCall.query)
            val actualVariables = actualCall.variables

            val matchingCalls = unmatchedExpectedCalls
                .filter { expected ->
                    actualCall.service == expected.service
                        && getCanonicalQuery(expected.query) == actualQuery
                        && jsonObjectMapper.readValue<JsonMap>(expected.variables) == actualVariables
                }

            // Multiple matches is ok, we match one at a time though
            if (matchingCalls.isNotEmpty()) {
                unmatchedExpectedCalls.remove(matchingCalls.first())
                iterator.remove()
            }

            // todo: match delayed responses here too
        }

        assertTrue(unmatchedExpectedCalls.isEmpty() && unmatchedActualCalls.isEmpty())
    }

    private suspend fun assertNadelResult(result: ExecutionResult, testData: TestData) {
        JSONAssert.assertEquals(
            testData.response.response,
            jsonObjectMapper.writeValueAsString(result.toSpecification()),
            JSONCompareMode.STRICT,
        )

        if (testData.response.delayedResponses.isEmpty()) {
            if (result is IncrementalExecutionResult) {
                assertTrue(result.incrementalItemPublisher.asFlow().toList().isEmpty())
                assertFalse(result.hasNext())
            }
        } else {
            // Note: there exists a IncrementalExecutionResult.getIncremental but that is part of the initial result
            assertTrue(result is IncrementalExecutionResult)

            val actualDelayedResponses = result.incrementalItemPublisher
                .asFlow()
                .toList()

            // Should only have one element that says hasNext=false, and it should be the last one
            assertTrue(actualDelayedResponses.dropLast(n = 1).all { it.hasNext() })
            assertFalse(actualDelayedResponses.last().hasNext())

            val (
                unmatchedExpectedDelayedResponses,
                unmatchedActualDelayedResponses,
            ) = findUnmatchedDelayedResponses(
                expectedResponses = testData.response.delayedResponses,
                actualResponses = actualDelayedResponses,
            )

            assertTrue(unmatchedExpectedDelayedResponses.isEmpty() && unmatchedActualDelayedResponses.isEmpty())
        }
    }

    /**
     * This is specifically for root level delayed responses that Nadel returns.
     */
    private fun findUnmatchedDelayedResponses(
        expectedResponses: List<String>, // Json object strings
        actualResponses: List<DelayedIncrementalPartialResult>,
    ): Pair<List<String>, List<String>> {
        val unmatchedExpectedDelayedResponses = expectedResponses
            .map { delayedResponse ->
                // We don't care about the order
                val withoutHasNext = jsonObjectMapper.readValue<MutableJsonMap>(delayedResponse)
                    .also {
                        it.remove("hasNext")
                    }

                jsonObjectMapper
                    .withPrettierPrinter()
                    .writeValueAsString(withoutHasNext)
            }
            .toMutableList()

        val unmatchedActualDelayedResponses = actualResponses
            .map { delayedResult ->
                jsonObjectMapper
                    .withPrettierPrinter()
                    .writeValueAsString(
                        delayedResult.toSpecification()
                            .also {
                                // Don't assert hasNext, we don't care about the order
                                it.remove("hasNext")
                            },
                    )
            }
            .toMutableList()

        // Matching process, this will remove matches from the unmatched lists above
        unmatchedActualDelayedResponses
            .forEachElementInIterator { iterator, actualResponse ->
                val matches = unmatchedExpectedDelayedResponses
                    .filter { expectedResponse ->
                        JSONCompare
                            .compareJSON(
                                expectedResponse,
                                actualResponse,
                                JSONCompareMode.STRICT
                            )
                            .passed()
                    }

                // Multiple matches is ok, we match one at a time though
                if (matches.isNotEmpty()) {
                    unmatchedExpectedDelayedResponses.remove(matches.first())
                    iterator.remove()
                }
            }

        return unmatchedExpectedDelayedResponses to unmatchedActualDelayedResponses
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