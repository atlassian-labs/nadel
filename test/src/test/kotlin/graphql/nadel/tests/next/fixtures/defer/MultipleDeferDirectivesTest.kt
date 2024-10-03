package graphql.nadel.tests.next.fixtures.defer

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

open class MultipleDeferDirectivesTest : NadelIntegrationTest(
    query = """
      query {
        defer {
          fastField
          ... @defer {
            slowField
          }
          ... @defer {
            anotherSlowField
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
                  fastField: ID
                  slowField: String
                  anotherSlowField: Int
                }
               
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class DeferApi(
                    val fastField: Int,
                    val slowField: String,
                    val anotherSlowField: Int
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
                            .dataFetcher("fastField") { env ->
                                123
                            }
                            .dataFetcher("slowField") { env ->
                                "slowString"
                            }
                            .dataFetcher("anotherSlowField") { env ->
                                123456789
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
