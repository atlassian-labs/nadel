package graphql.nadel

import graphql.ExecutionResult
import graphql.introspection.GoodFaithIntrospection
import graphql.validation.QueryComplexityLimits
import graphql.validation.ValidationError
import graphql.validation.ValidationErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture.completedFuture
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class NadelTest {
    private val serviceExecution = mockk<ServiceExecution>()
    private val schema = """
        type Query {
            echo: Echo
        }
        type Echo {
            value: String
        }
    """.trimIndent()
    private val nadel = Nadel.newNadel()
        .overallSchema("echo", schema)
        .underlyingSchema("echo", schema)
        .serviceExecutionFactory { serviceExecution }
        .build()

    @AfterEach
    fun close() {
        nadel.close()
    }

    @Test
    fun `passes request query depth limit to document validation`() {
        assertLimitIsEnforced(
            restrictiveLimits = QueryComplexityLimits.newLimits().maxDepth(1).build(),
            permissiveLimits = QueryComplexityLimits.newLimits().maxDepth(2).build(),
            expectedError = ValidationErrorType.MaxQueryDepthExceeded,
        )
    }

    @Test
    fun `passes request field count limit to document validation`() {
        assertLimitIsEnforced(
            restrictiveLimits = QueryComplexityLimits.newLimits().maxFieldsCount(1).build(),
            permissiveLimits = QueryComplexityLimits.newLimits().maxFieldsCount(2).build(),
            expectedError = ValidationErrorType.MaxQueryFieldsExceeded,
        )
    }

    @Test
    fun `validates using default limits when request has no query complexity limits`() {
        stubServiceExecution()

        assertSuccessfulExecution(execute())
        verify(exactly = 1) { serviceExecution.execute(any()) }
    }

    @Test
    fun `returns an error when good faith introspection validation rejects the query`() {
        val result = nadel.execute(repeatedIntrospectionInput().build()).join()

        val error = assertIs<GoodFaithIntrospection.BadFaithIntrospectionError>(result.errors.single())
        assertEquals(GoodFaithIntrospection.BadFaithIntrospectionError.tooManyFields("Query.__type").message, error.message)
        assertNull(result.getData<Any?>())
        verify(exactly = 0) { serviceExecution.execute(any()) }
    }

    @Test
    fun `can disable good faith introspection for a request`() {
        val input = repeatedIntrospectionInput()
            .graphqlContext(GoodFaithIntrospection.GOOD_FAITH_INTROSPECTION_DISABLED, true)
            .build()

        val result = nadel.execute(input).join()

        assertEquals(emptyList(), result.errors)
        assertEquals(
            mapOf("first" to mapOf("name" to "Echo"), "second" to mapOf("name" to "Echo")),
            result.getData<Any?>(),
        )
        verify(exactly = 0) { serviceExecution.execute(any()) }
    }

    @Test
    fun `disabling good faith introspection still enforces request complexity limits`() {
        val input = repeatedIntrospectionInput()
            .graphqlContext(GoodFaithIntrospection.GOOD_FAITH_INTROSPECTION_DISABLED, true)
            .graphqlContext(QueryComplexityLimits.KEY, QueryComplexityLimits.newLimits().maxFieldsCount(1).build())
            .build()

        val result = nadel.execute(input).join()

        assertEquals(
            ValidationErrorType.MaxQueryFieldsExceeded,
            assertIs<ValidationError>(result.errors.single()).validationErrorType,
        )
        assertNull(result.getData<Any?>())
        verify(exactly = 0) { serviceExecution.execute(any()) }
    }

    private fun repeatedIntrospectionInput(): NadelExecutionInput.Builder {
        return NadelExecutionInput.newNadelExecutionInput()
            .query("""{ first: __type(name: "Echo") { name } second: __type(name: "Echo") { name } }""")
    }

    private fun assertLimitIsEnforced(
        restrictiveLimits: QueryComplexityLimits,
        permissiveLimits: QueryComplexityLimits,
        expectedError: ValidationErrorType,
    ) {
        val rejected = execute(restrictiveLimits)

        assertEquals(expectedError, assertIs<ValidationError>(rejected.errors.single()).validationErrorType)
        assertNull(rejected.getData<Any?>())
        verify(exactly = 0) { serviceExecution.execute(any()) }

        // The same query succeeds with a different request's limits on the same Nadel instance.
        stubServiceExecution()
        assertSuccessfulExecution(execute(permissiveLimits))
        verify(exactly = 1) { serviceExecution.execute(any()) }
    }

    private fun execute(limits: QueryComplexityLimits? = null): ExecutionResult {
        val input = NadelExecutionInput.newNadelExecutionInput()
            .query("{ echo { value } }")
        if (limits != null) {
            // Nadel copies this context into ExecutionInput, then supplies these limits to
            // Validator.validateDocument(schema, document, rulePredicate, locale, limits).
            input.graphqlContext(QueryComplexityLimits.KEY, limits)
        }
        return nadel.execute(input.build()).join()
    }

    private fun stubServiceExecution() {
        every { serviceExecution.execute(any()) } answers {
            completedFuture(
                NadelServiceExecutionResultImpl(
                    data = linkedMapOf("echo" to linkedMapOf("value" to "hello")),
                ),
            )
        }
    }

    private fun assertSuccessfulExecution(result: ExecutionResult) {
        assertEquals(emptyList(), result.errors)
        assertEquals(mapOf("echo" to mapOf("value" to "hello")), result.getData<Any?>())
    }
}
