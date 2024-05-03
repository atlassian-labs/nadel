package graphql.nadel.tests.next.fixtures.hydration.defer

import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.tests.next.NadelIntegrationTest

class HydrationDeferInRenamedFieldTest : NadelIntegrationTest(
    query = """
      query {
        issueByKey(key: "GQLGW-1") {
          key
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
            name = "issues",
            overallSchema = """
              directive @defer(if: Boolean, label: String) on FRAGMENT_SPREAD | INLINE_FRAGMENT
              type Query {
                issueById(id: ID!): Issue @renamed(from: "getIssueById")
                issueByKey(key: String!): Issue @renamed(from: "getIssueByKey")
              }
              type Issue {
                id: ID!
                key: String!
                assigneeId: ID @hidden
                assignee: User
                  @hydrated(
                    service: "users"
                    field: "userById"
                    arguments: [{name: "id", value: "$source.assigneeId"}]
                  )
              }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(
                    val id: Int,
                    val key: String,
                    val assigneeId: String,
                )

                val issues = listOf(
                    Issue(
                        id = 1,
                        key = "GQLGW-1",
                        assigneeId = "ari:cloud:identity::user/1",
                    ),
                )

                val issuesById = issues.strictAssociateBy { it.id }
                val issuesByKey = issues.strictAssociateBy { it.key }

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("getIssueByKey") { env ->
                                issuesByKey[env.getArgument("key")]
                            }
                            .dataFetcher("getIssueById") { env ->
                                issuesById[env.getArgument<String>("id").toIntOrNull()]
                            }
                    }
            },
        ),
        Service(
            name = "users",
            overallSchema = """
              type Query {
                userById(id: ID!): User
              }
              type User {
                id: ID!
                name: String
              }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class User(
                    val id: String,
                    val name: String,
                )

                val users = listOf(
                    User(
                        id = "ari:cloud:identity::user/1",
                        name = "Franklin",
                    ),
                )
                val usersById = users.strictAssociateBy { it.id }

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("userById") { env ->
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
