package graphql.nadel.tests.next.fixtures.hydration.defer

import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.incremental.IncrementalExecutionResult
import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.tests.next.NadelIntegrationTest
import org.intellij.lang.annotations.Language
import kotlin.test.assertTrue

class HydrationDeferIsDisabledForNestedHydrationsTest : HydrationDeferIsDisabled(
    query = """
        query {
          issueByKey(key: "GQLGW-3") { # Not a list
            key
            self { # Hydration
              ... @defer {
                assignee { # Should NOT defer
                  name
                }
              }
            }
          }
        }
    """.trimIndent(),
) {
    override fun assert(result: ExecutionResult, incrementalResults: List<DelayedIncrementalPartialResult>?) {
        assertTrue(result !is IncrementalExecutionResult)
        assertTrue(incrementalResults == null)
    }
}

abstract class HydrationDeferIsDisabled(
    @Language("GraphQL")
    query: String,
) : NadelIntegrationTest(
    query = query,
    services = listOf(
        Service(
            name = "issues",
            overallSchema = """
              type Query {
                issues: [Issue!]
                issueByKey(key: String!): Issue
              }
              type Issue {
                key: String!
                assigneeId: ID!
                self: Issue
                  @hydrated(
                    field: "issueByKey"
                    arguments: [{name: "key", value: "$source.key"}]
                  )
                assignee: User
                  @hydrated(
                    field: "userById"
                    arguments: [{name: "id", value: "$source.assigneeId"}]
                  )
                related: [Issue!]
                parent: Issue
              }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(
                    val key: String,
                    val assigneeId: String,
                    val parentKey: String? = null,
                    val relatedKeys: List<String> = emptyList(),
                )

                val issues = listOf(
                    Issue(
                        key = "GQLGW-1",
                        assigneeId = "ari:cloud:identity::user/1",
                    ),
                    Issue(
                        key = "GQLGW-2",
                        assigneeId = "ari:cloud:identity::user/2",
                        relatedKeys = listOf("GQLGW-1"),
                        parentKey = "GQLGW-1",
                    ),
                    Issue(
                        key = "GQLGW-3",
                        assigneeId = "ari:cloud:identity::user/1",
                        parentKey = "GQLGW-1",
                        relatedKeys = listOf("GQLGW-1", "GQLGW-2"),
                    ),
                )
                val issuesByKey = issues.strictAssociateBy { it.key }

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("issueByKey") { env ->
                                issuesByKey[env.getArgument("key")]
                            }
                            .dataFetcher("issues") { env ->
                                issues
                            }
                    }
                    .type("Issue") { type ->
                        type
                            .dataFetcher("related") { env ->
                                env.getSource<Issue>()!!
                                    .relatedKeys
                                    .map {
                                        issuesByKey[it]!!
                                    }
                            }
                            .dataFetcher("parent") { env ->
                                issuesByKey[env.getSource<Issue>()!!.parentKey]
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
                name: String!
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
                    User(
                        id = "ari:cloud:identity::user/2",
                        name = "Tom",
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
