package graphql.nadel.tests.next.fixtures.defer.transforms

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

/*
 * This tests the NadelServiceTypeFilterTransform with defer
 */
open class DeferredServiceTypeFilterTest : NadelIntegrationTest(
    query = """
      query {
        aErrors {
          ... on BError {
            id
          }
        }
      }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "defer",
            overallSchema = """
                service shared {
                  interface Error { id: ID }
                }
                service A {
                  type Query {
                     aErrors: [Error]
                  }
                  type AError implements Error { id: ID }
                }
                service B {
                  type BError implements Error {
                    id: ID
                    b: String
                  }
                }

            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("aErrors") { env ->
                                Any()
                            }
                    }
                    .type("AError") { type ->
                        type
                            .dataFetcher("id") { env ->
                                "A-ERROR-1"
                            }
                    }
                    .type("BError") { type ->
                        type
                            .dataFetcher("id") { env ->
                                "B-ERROR-1"
                            }
                    }
            },
        ),
    ),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .deferSupport { true }
    }
}
