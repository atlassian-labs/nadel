package graphql.nadel.tests.next.fixtures

import graphql.execution.ResultPath
import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.tests.next.NadelIntegrationTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import kotlin.time.Duration.Companion.milliseconds

class UnderlyingServiceDeferTest : NadelIntegrationTest(
    query = """
        query {
          echo
          users {
            name
            ... @defer {
              friends {
                name
                ... @defer {
                  friends {
                    name
                  }
                }
              }
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "echo",
            overallSchema = """
              directive @defer(if: Boolean, label: String) on FRAGMENT_SPREAD | INLINE_FRAGMENT
              type Query {
                echo: String
              }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("echo") {
                                "Hello World"
                            }
                    }
            },
        ),
        Service(
            name = "users",
            overallSchema = """
              type Query {
                users: [User]
              }
              type User {
                id: ID!
                name: String!
                friends: [User!]
              }
            """.trimIndent(),
            underlyingSchema = """
              directive @defer(if: Boolean, label: String) on FRAGMENT_SPREAD | INLINE_FRAGMENT
              type Query {
                users: [User]
              }
              type User {
                id: ID!
                name: String!
                friends: [User!]
              }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class User(
                    val id: String,
                    val name: String,
                )

                val usersByIds = listOf(
                    User(
                        id = "1",
                        name = "Johnny",
                    ),
                    User(
                        id = "2",
                        name = "Bert",
                    ),
                ).strictAssociateBy { it.id }

                val coroutineScope = CoroutineScope(SupervisorJob())

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("users") {
                                usersByIds.values
                            }
                    }
                    .type("User") { type ->
                        type
                            .dataFetcher("friends") { env ->
                                val self = env.getSource<User>()
                                coroutineScope
                                    .future {
                                        usersByIds.values.filterNot { it.id == self.id }
                                    }
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
