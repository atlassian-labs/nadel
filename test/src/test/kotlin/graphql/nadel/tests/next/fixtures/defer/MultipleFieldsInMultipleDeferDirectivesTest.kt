package graphql.nadel.tests.next.fixtures.defer

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

open class MultipleFieldsInMultipleDeferDirectivesTest : NadelIntegrationTest(
    query = """
      query {
        defer {
          fastField
          ... @defer {
            slowField
            slowField2
          }
          ... @defer {
            slowField3
            slowField4
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
                  slowField2: String
                  slowField3: String
                  slowField4: String
                }
               
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class DeferApi(
                    val fastField: Int,
                    val slowField: String,
                    val slowField2: String,
                    val slowField3: String,
                    val slowField4: String,
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
                            .dataFetcher("slowField2") { env ->
                                "slowString2"
                            }
                            .dataFetcher("slowField3") { env ->
                                "slowString3"
                            }
                            .dataFetcher("slowField4") { env ->
                                "slowString4"
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
