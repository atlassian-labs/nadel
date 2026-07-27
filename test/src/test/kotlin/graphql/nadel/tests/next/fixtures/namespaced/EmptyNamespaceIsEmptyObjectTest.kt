package graphql.nadel.tests.next.fixtures.namespaced

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * Regression test for the HOT/bug where an empty namespace was materialised as `"jira": null`.
 *
 * When every child of a namespaced field is skipped (e.g. `@skip(if: true)`), the namespace has no
 * children and no service call is made. It must be materialised as an empty object `"jira": {}`,
 * NOT as `"jira": null` (which tripped a downstream Relay non-null handler, turning the whole
 * response into `data: null`).
 *
 * Expected result: `{ "hello": "world", "jira": {} }`.
 */
class EmptyNamespaceIsEmptyObjectTest : NadelIntegrationTest(
    query = """
        query {
          hello
          jira {
            sprint @skip(if: true)
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "monolith",
            overallSchema = """
                type Query {
                  hello: String
                  jira: JiraQuery @namespaced
                }
                type JiraQuery {
                  sprint: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  hello: String
                  jira: JiraQuery
                }
                type JiraQuery {
                  sprint: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("hello") { "world" }
                            .dataFetcher("jira") { emptyMap<String, Any?>() }
                    }
            },
        ),
    ),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .newResultMergerAndNamespacedTypename { true }
    }
}
