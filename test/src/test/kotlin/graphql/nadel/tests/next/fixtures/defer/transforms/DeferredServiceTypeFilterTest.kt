package graphql.nadel.tests.next.fixtures.defer.transforms

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * This tests the NadelServiceTypeFilterTransform with defer
 */
open class DeferredServiceTypeFilterTest : NadelIntegrationTest(
    query = """
      query {
        aErrors {
          ...@defer {
            ...on AError {
                id
            }
            ...on BError {
              id
            }
          }
        }
      }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "A",
            overallSchema = """
                type Query {
                    aErrors: Error
                }
                type AError implements Error { id: ID }
                interface Error { id: ID }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                    aErrors: Error
                }
                type AError implements Error { id: ID }
                interface Error { id: ID } 
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
                    .type("Error") { type ->
                        type
                            .typeResolver { env ->
                                env.schema.getObjectType("AError")
                            }
                    }
            },
        ),
        Service(
            name = "B",
            overallSchema = """
                type Query {
                  echo: String
                }
                type BError implements Error {
                  id: ID
                  b: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  echo: String
                }
                type BError implements Error {
                  id: ID
                  b: String
                }
                interface Error { id: ID }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Error") { type ->
                        type
                            .typeResolver { env ->
                                env.schema.getObjectType("BError")
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
