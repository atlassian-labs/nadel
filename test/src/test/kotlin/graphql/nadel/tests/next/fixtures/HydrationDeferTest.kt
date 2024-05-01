package graphql.nadel.tests.next.fixtures

import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.tests.next.NadelIntegrationTest

class HydrationDeferTest : NadelIntegrationTest(
    query = """
        query {
          issue(id: "ari:cloud:jira::issue/1") {
            id
            ... @defer {
              assignee {
                name
              }
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
            underlyingSchema = """
                directive @defer(if: Boolean, label: String) on FRAGMENT_SPREAD | INLINE_FRAGMENT
                type Query {
                  echo: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
        Service(
            name = "issues",
            overallSchema = """
                type Query {
                  issue(id: ID!): Issue
                }
                type Issue {
                  id: ID!
                  assigneeId: ID! @hidden
                  assignee: User
                    @hydrated(
                      service: "users"
                      field: "user"
                      arguments: [{name: "id", value: "$source.assigneeId"}]
                    )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  issue(id: ID!): Issue
                }
                type Issue {
                  id: ID!
                  assigneeId: ID!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(
                    val id: String,
                    val assigneeId: String,
                )

                val issuesById: Map<String, Issue> = listOf(
                    Issue(
                        id = "ari:cloud:jira::issue/1",
                        assigneeId = "ari:cloud:jira::user/1",
                    ),
                ).strictAssociateBy { it.id }

                wiring
                    .type("Query") { type ->
                        type.dataFetcher("issue") { env ->
                            issuesById[env.getArgument("id")]
                        }
                    }
            },
        ),
        Service(
            name = "users",
            overallSchema = """
                type Query {
                  user(id: ID!): User
                }
                type User {
                  id: ID!
                  name: String!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class User(
                    val id: String,
                    val name: String,
                )

                val usersById = listOf(
                    User(
                        id = "ari:cloud:jira::user/1",
                        name = "Franklin",
                    ),
                ).strictAssociateBy { it.id }

                wiring
                    .type("Query") { type ->
                        type.dataFetcher("user") { env ->
                            usersById[env.getArgument("id")]
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
