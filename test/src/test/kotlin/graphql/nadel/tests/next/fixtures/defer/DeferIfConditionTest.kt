package graphql.nadel.tests.next.fixtures.defer

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

open class DeferIfConditionTest : NadelIntegrationTest(
    query = """
      query {
        defer {
          hello
          ... @defer {
            slow1
          }
          ... @defer(if: false) {
            slow2
          }
          ... @defer(if: true) {
            slow3
          }
          ... @defer(if: false label: "thisBoiFalse") {
            slow3
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
                  slow3: String
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
                            .dataFetcher("hello") { env ->
                                "helloString"
                            }
                            .dataFetcher("slow1") { env ->
                                "slowString 1"
                            }
                            .dataFetcher("slow2") { env ->
                                "slowString 2"
                            }
                            .dataFetcher("slow3") { env ->
                                "slowString 3"
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
