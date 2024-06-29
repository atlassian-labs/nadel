package graphql.nadel.tests.next.fixtures.defer

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

open class DeferWithIfFalseTest : NadelIntegrationTest(
    query = """
      query {
        defer {
          hello
          ... @defer(if: false) {
            slow
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
                  slow: String
                }
               
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class DeferApi(
                    val hello: String,
                    val slow: String,
                )

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
                            .dataFetcher("slow") { env ->
                                "slowString"
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
