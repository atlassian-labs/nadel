package graphql.nadel.tests.next.fixtures.defer

import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.tests.next.NadelIntegrationTest

open class SimpleDeferTest : NadelIntegrationTest(
    query = """
      query {
        defer {
          hello
          ... @defer(label: "slow-defer") {
            slow
          }
        }
      }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "shared",
            overallSchema = """
                directive @defer(if: Boolean, label: String) on FRAGMENT_SPREAD | INLINE_FRAGMENT
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
        Service(
            name = "defer",
            overallSchema = """

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
                        type.dataFetcher("defer") {
                            DeferApi(
                                "helloString",
                                "slowString"
                            )
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
