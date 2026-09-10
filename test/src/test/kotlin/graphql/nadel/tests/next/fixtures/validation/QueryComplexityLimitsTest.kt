package graphql.nadel.tests.next.fixtures.validation

import graphql.nadel.NadelExecutionInput
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.validation.QueryComplexityLimits
import org.intellij.lang.annotations.Language

class QueryDepthLimitExceededTest : QueryValidationTestBase(
    limits = QueryComplexityLimits.newLimits().maxDepth(1).build(),
)

class QueryDepthAtLimitTest : QueryValidationTestBase(
    limits = QueryComplexityLimits.newLimits().maxDepth(2).build(),
)

class QueryFieldCountLimitExceededTest : QueryValidationTestBase(
    limits = QueryComplexityLimits.newLimits().maxFieldsCount(1).build(),
)

class QueryFieldCountAtLimitTest : QueryValidationTestBase(
    limits = QueryComplexityLimits.newLimits().maxFieldsCount(2).build(),
)

class QueryComplexityDefaultLimitsTest : QueryValidationTestBase()

abstract class QueryValidationTestBase(
    @Language("GraphQL") query: String = "{ echo { value } }",
    private val limits: QueryComplexityLimits? = null,
) : NadelIntegrationTest(
    query = query,
    services = listOf(
        Service(
            name = "echo",
            overallSchema = """
                type Query {
                  echo: Echo
                }
                type Echo {
                  value: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("echo") { _ ->
                        mapOf("value" to "hello")
                    }
                }
            },
        ),
    ),
) {
    override fun makeExecutionInput(): NadelExecutionInput.Builder {
        val input = super.makeExecutionInput()
        if (limits != null) {
            // Nadel passes these request limits to Validator.validateDocument.
            input.graphqlContext(QueryComplexityLimits.KEY, limits)
        }
        return input
    }
}
