package graphql.nadel.tests.next.fixtures.hydration.defer

import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.tests.next.NadelIntegrationTest
import org.intellij.lang.annotations.Language

class HydrationDeferInRenamedFieldTest : BaseHydrationDeferInRenamedFieldTest(
    query = """
      query {
        issueByKey(key: "GQLGW-1") { # Renamed
          key
          ... @defer {
            assignee {
              name
            }
          }
        }
      }
    """.trimIndent(),
)

class HydrationRenamedFieldInDeferTest : BaseHydrationDeferInRenamedFieldTest(
    query = """
      query {
        ... @defer {
          issueByKey(key: "GQLGW-1") { # Renamed
            key
              assignee {
                name
              }
          }
        }
      }
    """.trimIndent(),
)

class HydrationDeferInRenamedFieldUsingRenamedFieldTest : BaseHydrationDeferInRenamedFieldTest(
    query = """
      query {
        issueByKey(key: "GQLGW-1") { # Renamed
          key
          ... @defer {
            assigneeV2 { # Renamed
              name
            }
          }
        }
      }
    """.trimIndent(),
)

/**
 * `@defer` hydration is currently disabled for nested hydrations.
 */
class HydrationDeferInRenamedFieldAndNestedHydrationsTest : BaseHydrationDeferInRenamedFieldTest(
    query = """
      query {
        issueByKey(key: "GQLGW-1") { # Renamed
          key
          self { # Hydrate
            self { # Hydrate
              self { # Hydrate
                ... @defer {
                  assigneeV2 { # Renamed
                    name
                  }
                }
              }
            }
          }
        }
      }
    """.trimIndent(),
)

class RenamedFieldInsideNestedHydrationsInsideDeferTest : BaseHydrationDeferInRenamedFieldTest(
    query = """
      query {
        issueByKey(key: "GQLGW-1") { # Renamed
          key
          ... @defer {
            self { # Hydrate
              self { # Hydrate
                self { # Hydrate
                    assigneeV2 { # Renamed
                      name
                    }
                }
              }
            }
          }
        }
      }
    """.trimIndent(),
)

abstract class BaseHydrationDeferInRenamedFieldTest(
    @Language("GraphQL")
    query: String,
) : NadelIntegrationTest(
    query = query,
    services = listOf(
        Service(
            name = "issues",
            overallSchema = """
              type Query {
                issueById(id: ID!): Issue @renamed(from: "getIssueById")
                issueByKey(key: String!): Issue @renamed(from: "getIssueByKey")
              }
              type Issue {
                id: ID!
                key: String!
                assigneeId: ID @hidden
                self: Issue
                  @hydrated(
                    service: "issues"
                    field: "issueById"
                    arguments: [{name: "id", value: "$source.id"}]
                  )
                assignee: User
                  @hydrated(
                    service: "users"
                    field: "userById"
                    arguments: [{name: "id", value: "$source.assigneeId"}]
                  )
                assigneeV2: User
                  @hydrated(
                    service: "users"
                    field: "quickUser"
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
                                issuesById[env.getArgument<String>("id")!!.toIntOrNull()]
                            }
                    }
            },
        ),
        Service(
            name = "users",
            overallSchema = """
              type Query {
                userById(id: ID!): User
                quickUser(id: ID!): User @renamed(from: "user_fast")
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
                            .dataFetcher("user_fast") { env ->
                                usersById[env.getArgument("id")]
                                    ?.copy(name = "SPEED")
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
