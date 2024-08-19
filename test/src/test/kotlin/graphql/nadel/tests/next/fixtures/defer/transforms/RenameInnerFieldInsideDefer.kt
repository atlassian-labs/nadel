package graphql.nadel.tests.next.fixtures.defer.transforms

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

open class RenameInnerFieldInsideDefer : NadelIntegrationTest(
    query = """
      query {
        defer {
          ...@defer(if: true) {
              user {
                firstName
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
                  user: User
                }
                type User {
                  firstName: String @renamed(from: "name")
                  lastName: String
                }

            """.trimIndent(),
            underlyingSchema = """
                directive @defer(if: Boolean, label: String) on FRAGMENT_SPREAD | INLINE_FRAGMENT

                type Query {
                  defer: DeferApi
                }
                type DeferApi {
                  user: User
                }
                type User {
                  name: String
                  lastName: String
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
                            .dataFetcher("user") { env ->
                                Any()
                            }
                    }
                    .type("User") { type ->
                        type
                            .dataFetcher("name") { env ->
                                "Steven"
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