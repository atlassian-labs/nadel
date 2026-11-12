package graphql.nadel

import graphql.GraphQLContext
import graphql.execution.ExecutionId
import graphql.language.Document
import graphql.language.OperationDefinition
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelServiceExecutionContext
import graphql.normalized.ExecutableNormalizedField
import io.mockk.coVerifyOrder
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ServiceExecutionParametersTest {
    @Test
    fun `singular accessor returns the only root`() {
        val root = field("user")

        assertFields(listOf(root), root)
    }

    @Test
    fun `singular accessor returns the first of multiple roots`() {
        val first = field("user")
        val second = field("team")

        assertFields(listOf(first, second), first)
    }

    @Test
    fun `singular accessor skips leading typename roots`() {
        val typename = field("__typename")
        val first = field("user")
        val second = field("team")

        assertFields(listOf(typename, first, second), first)
    }

    @Test
    fun `singular accessor returns the first root when all roots are typename`() {
        val first = field("__typename")
        val second = field("__typename")

        assertFields(listOf(first, second), first)
    }

    @Suppress("DEPRECATION")
    private fun assertFields(
        fields: List<ExecutableNormalizedField>,
        expectedSingular: ExecutableNormalizedField,
    ) {
        val executionContext = mockk<NadelExecutionContext>()
        val parameters = ServiceExecutionParameters(
            query = Document.newDocument().build(),
            context = null,
            graphQLContext = GraphQLContext.newContext().build(),
            variables = emptyMap(),
            operationDefinition = OperationDefinition.newOperationDefinition()
                .operation(OperationDefinition.Operation.QUERY)
                .build(),
            executionId = ExecutionId.from("service-execution-parameters-test"),
            executionContext = executionContext,
            serviceExecutionContext = NadelServiceExecutionContext.None,
            hydrationDetails = null,
            executableNormalizedFields = fields,
        )

        assertSame(executionContext, parameters.executionContext)
        assertSame(expectedSingular, parameters.executableNormalizedField)
        assertEquals(fields.size, parameters.executableNormalizedFields.size)
        fields.forEachIndexed { index, field ->
            assertSame(field, parameters.executableNormalizedFields[index])
        }
        coVerifyOrder(inverse = true) {
            executionContext.hints
        }
        confirmVerified(executionContext)
    }

    private fun field(name: String): ExecutableNormalizedField {
        return ExecutableNormalizedField.newNormalizedField()
            .fieldName(name)
            .objectTypeNames(listOf("Query"))
            .build()
    }
}
