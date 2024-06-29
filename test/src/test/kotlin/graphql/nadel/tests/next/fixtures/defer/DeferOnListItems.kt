package graphql.nadel.tests.next.fixtures.defer

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

open class DeferOnListItemsTest : NadelIntegrationTest(
    query = """
      query {
        defer {
            list {
              fast
              ... @defer {
                slow
              }
            }
        }
      }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "defer",
            overallSchema = """
                directive @defer(if: Boolean, label: String) on FRAGMENT_SPREAD | INLINE_FRAGMENT

                type Query {
                  defer: DeferApi
                }
                type DeferApi {
                  list: [Test] 
                }
                
                type Test {
                  fast: String
                  slow: String
                }
               
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("defer") { env ->
                                Any()
                            }
                    }
                    .type("DeferApi") { type ->
                        type
                            .dataFetcher("list") { env ->
                                listOf(
                                    mapOf<String, Any>(),
                                    mapOf<String, Any>(),
                                )
                            }
                    }
                    .type("Test") { type ->
                        type
                            .dataFetcher("fast") { env ->
                                "fastString"
                            }
                            .dataFetcher("slow") { env ->
                                "slowString"
                            }
                    }
            }
        ),
    ),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .deferSupport { true }
    }
}
