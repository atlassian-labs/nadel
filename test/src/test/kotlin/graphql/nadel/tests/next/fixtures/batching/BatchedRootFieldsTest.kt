package graphql.nadel.tests.next.fixtures.batching

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * Multiple sibling root fields destined for the same service are combined into a single service
 * call when [NadelExecutionHints.batchRootFields] is enabled for that service.
 *
 * The snapshot should record a single call to "monolith" containing foo, bar and baz.
 */
class BatchedRootFieldsTest : NadelIntegrationTest(
    query = """
        query {
          foo
          bar
          baz
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "monolith",
            overallSchema = """
                type Query {
                  foo: String
                  bar: String
                  baz: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("foo") { "foo-value" }
                            .dataFetcher("bar") { "bar-value" }
                            .dataFetcher("baz") { "baz-value" }
                    }
            },
        ),
    ),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .batchRootFields { true }
    }
}
