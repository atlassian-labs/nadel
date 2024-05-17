package graphql.nadel.tests.next.fixtures.defer

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

@ignore
open class NestedDeferDirectivesTest : NadelIntegrationTest(
    query = """
      query {
        defer {
          hello
          ... @defer(label: "outer defer") {
            slow1
            ... @defer(label: "inner defer") {
                slow2
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
                  hello: String
                  slow1: String
                  slow2: String
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
                            .dataFetcher("fast") { env ->
                                "fastString"
                            }
                            .dataFetcher("slow1") { env ->

                                "slowString 1"
                            }
                            .dataFetcher("slow2") { env ->
                                Thread.sleep(1_000)  // wait for 1 second
                                "slowString 2"
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
