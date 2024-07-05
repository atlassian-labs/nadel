package graphql.nadel.tests.next.fixtures.hydration.defer

import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.tests.next.NadelIntegrationTest

class DeferGroupingTest : NadelIntegrationTest(
    query = """
query {
  issue(id: 1) {
    ... @defer {
      key
      id
    }
    
    ... @defer {
      key
    }
  }
}
    """.trimIndent(),
    services = listOf(
        Service(
            name = "monolith",
            overallSchema = """
                type Query {
                  issue(id: ID!): Issue
                  user(id: ID!): User
                }
                type Issue {
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
                type User {
                  id: ID!
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(
                    val id: Int,
                    val key: String,
                    val assigneeId: Int,
                )

                data class User(
                    val id: Int,
                    val name: String,
                )

                wiring
                    .type("Query") { type ->
                        val issueById = listOf(
                            Issue(
                                id = 1,
                                key = "TEST-1",
                                assigneeId = 1,
                            ),
                        ).strictAssociateBy { it.id }

                        val userById = listOf(
                            User(
                                id = 1,
                                name = "Tester",
                            ),
                        ).strictAssociateBy { it.id }

                        type
                            .dataFetcher("issue") { env ->
                                val id = env.getArgument<String>("id")!!.toInt()
                                issueById[id]
                            }
                            .dataFetcher("user") { env ->
                                val id = env.getArgument<String>("id")!!.toInt()
                                userById[id]
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
