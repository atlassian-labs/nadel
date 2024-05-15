package graphql.nadel.tests.next.fixtures.hydration.defer

import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.error.NadelGraphQLErrorException
import graphql.nadel.tests.next.NadelIntegrationTest
import org.intellij.lang.annotations.Language
import kotlin.test.assertTrue

class HydrationDeferForwardsErrorsTest : HydrationDeferForwardsErrors(
    query = """
        query {
          issueByKey(key: "GQLGW-2") {
            ... @defer {
              assignee {
                name
              }
            }
          }
        }
    """.trimIndent(),
) {
    override fun assert(result: ExecutionResult, incrementalResults: List<DelayedIncrementalPartialResult>?) {
        assertTrue(incrementalResults?.isNotEmpty() == true)
        assertTrue(incrementalResults?.single()?.incremental?.single()?.errors?.isNotEmpty() == true)
    }
}

abstract class HydrationDeferForwardsErrors(
    @Language("GraphQL")
    query: String,
) : NadelIntegrationTest(
    query = query,
    services = listOf(
        Service(
            name = "issues",
            overallSchema = """
              directive @defer(if: Boolean, label: String) on FRAGMENT_SPREAD | INLINE_FRAGMENT
              type Query {
                issues: [Issue!]
                issueGroups: [[Issue]]
                issueByKey(key: String!): Issue
              }
              type Issue {
                key: String!
                assigneeId: ID!
                self: Issue
                  @hydrated(
                    service: "issues"
                    field: "issueByKey"
                    arguments: [{name: "key", value: "$source.key"}]
                  )
                assignee: User
                  @hydrated(
                    service: "users"
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
                        assigneeId = "ari:cloud:identity::user/0",
                        parentKey = "GQLGW-1",
                        relatedKeys = listOf("GQLGW-1"),
                    ),
                    Issue(
                        key = "GQLGW-3",
                        assigneeId = "ari:cloud:identity::user/1",
                        parentKey = "GQLGW-1",
                        relatedKeys = listOf("GQLGW-1", "GQLGW-2"),
                    ),
                    Issue(
                        key = "GQLGW-4",
                        assigneeId = "ari:cloud:identity::user/3",
                        parentKey = "GQLGW-1",
                        relatedKeys = listOf("GQLGW-1", "GQLGW-2", "GQLGW-3"),
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
                            .dataFetcher("issueGroups") {
                                issues
                                    .groupBy {
                                        it.key.substringAfter("-").toInt() % 2 == 0
                                    }
                                    .values
                            }
                    }
                    .type("Issue") { type ->
                        type
                            .dataFetcher("related") { env ->
                                env.getSource<Issue>()
                                    .relatedKeys
                                    .map {
                                        issuesByKey[it]!!
                                    }
                            }
                            .dataFetcher("parent") { env ->
                                issuesByKey[env.getSource<Issue>().parentKey]
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
                        name = "Frank",
                    ),
                    User(
                        id = "ari:cloud:identity::user/2",
                        name = "Tom",
                    ),
                    User(
                        id = "ari:cloud:identity::user/3",
                        name = "Lin",
                    ),
                )
                val usersById = users.strictAssociateBy { it.id }

                class UserNotFoundError(id: String) : NadelGraphQLErrorException(message = "No user: $id")

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("userById") { env ->
                                val id = env.getArgument<String>("id")
                                usersById[id] ?: throw UserNotFoundError(id)
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
