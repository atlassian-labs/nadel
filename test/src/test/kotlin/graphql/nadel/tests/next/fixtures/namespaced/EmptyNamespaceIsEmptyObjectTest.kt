package graphql.nadel.tests.next.fixtures.namespaced

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * Regression test for the bug where an empty namespace was materialised as `"namespace": null`.
 *
 * When every child of a namespaced field is skipped (e.g. `@skip(if: true)`), the namespace has no
 * children and no service call is made. It must be materialised as an empty object `"namespace": {}`,
 * NOT as `"namespace": null` (which tripped a downstream non-null handler, turning the whole
 * response into `data: null`).
 *
 * Expected result: `{ "hello": "world", "namespace": {} }`.
 */
class EmptyNamespaceIsEmptyObjectTest : NadelIntegrationTest(
    query = """
        query {
          hello
          namespace {
            foo @skip(if: true)
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "monolith",
            overallSchema = """
                type Query {
                  hello: String
                  namespace: NamespaceQuery @namespaced
                }
                type NamespaceQuery {
                  foo: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  hello: String
                  namespace: NamespaceQuery
                }
                type NamespaceQuery {
                  foo: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("hello") { "world" }
                            .dataFetcher("namespace") { emptyMap<String, Any?>() }
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
