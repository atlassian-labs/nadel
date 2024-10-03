package graphql.nadel.tests.next.fixtures.hydration.defer

import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.tests.next.NadelIntegrationTest

class HydrationDeferInlineFragmentGroupingTest : NadelIntegrationTest(
    query = """
        query {
          node(id: "issue/1") {
            ... on Issue @defer {
              key
              assignee {
                name
              }
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "monolith",
            overallSchema = """
                type Query {
                  node(id: ID!): Node
                  user(id: ID!): User
                  issue(id: ID!): Issue
                }
                interface Node {
                  id: ID!
                }
                type Issue implements Node {
                  id: ID!
                  key: String!
                  assigneeId: ID @hidden
                  assignee: User
                    @hydrated(
                      service: "monolith"
                      field: "user"
                      arguments: [{name: "id", value: "$source.assigneeId"}]
                    )
                }
                type User implements Node {
                  id: ID!
                  name: String
                  laptopId: ID
                  laptop: Issue
                    @hydrated(
                      service: "monolith"
                      field: "issue"
                      arguments: [{name: "id", value: "$source.laptopId"}]
                    )
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(
                    override val id: String,
                    val key: String,
                    val assigneeId: String,
                ) : Node

                data class User(
                    override val id: String,
                    val name: String,
                ) : Node

                wiring
                    .type("Node") { type ->
                        type
                            .typeResolver {
                                when (it.getObject<Any>()) {
                                    is Issue -> it.schema.getObjectType("Issue")
                                    is User -> it.schema.getObjectType("User")
                                    else -> throw UnsupportedOperationException()
                                }
                            }
                    }
                    .type("Query") { type ->
                        fun issueId(id: Int) = "issue/$id"
                        fun userId(id: Int) = "user/$id"

                        val issueById = listOf(
                            Issue(
                                id = issueId(1),
                                key = "TEST-1",
                                assigneeId = userId(1),
                            ),
                        ).strictAssociateBy { it.id }

                        val userById = listOf(
                            User(
                                id = userId(1),
                                name = "Tester",
                            ),
                        ).strictAssociateBy { it.id }

                        val nodeById = (issueById.values + userById.values)
                            .strictAssociateBy { it.id }

                        type
                            .dataFetcher("nodes") { env ->
                                nodeById.values
                            }
                            .dataFetcher("node") { env ->
                                val id = env.getArgument<String>("id")
                                nodeById[id]
                            }
                            .dataFetcher("issue") { env ->
                                val id = env.getArgument<String>("id")
                                issueById[id]
                            }
                            .dataFetcher("user") { env ->
                                val id = env.getArgument<String>("id")
                                userById[id]
                            }
                    }
            },
        ),
    ),
) {
    interface Node {
        val id: String
    }

    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .deferSupport { true }
    }
}
