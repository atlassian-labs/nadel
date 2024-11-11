package graphql.nadel.tests.next.fixtures.hydration.defer.batch

import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.tests.next.NadelIntegrationTest

class BatchHydrationDeferWithLabelTest : NadelIntegrationTest(
    query = """
        query {
          issues {
            key
            ... @defer(label: "deferredAssignee") {
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
                  issues: [Issue!]
                }
                type Issue {
                  key: String!
                  assigneeId: ID! @hidden
                  assignee: User
                    @hydrated(
                      service: "users"
                      field: "usersByIds"
                      arguments: [{name: "ids", value: "$source.assigneeId"}]
                    )
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(
                    val key: String,
                    val assigneeId: String,
                )

                val issues = listOf(
                    Issue(
                        key = "GQLGW-1",
                        assigneeId = "fwang",
                    ),
                    Issue(
                        key = "GQLGW-2",
                        assigneeId = "sbarker2",
                    ),
                    Issue(
                        key = "GQLGW-3",
                        assigneeId = "freis",
                    ),
                )

                wiring
                    .type("Query") { type ->
                        type.dataFetcher("issues") { env ->
                            issues
                        }
                    }
            }
        ),
        Service(
            name = "users",
            overallSchema = """
                type Query {
                  usersByIds(ids: [ID!]!): [User!]
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
                        id = "fwang",
                        name = "Franklin",
                    ),
                    User(
                        id = "sbarker2",
                        name = "Steven",
                    ),
                    User(
                        id = "freis",
                        name = "Felipe",
                    ),
                )
                val usersByIds = users.strictAssociateBy { it.id }

                wiring
                    .type("Query") { type ->
                        type.dataFetcher("usersByIds") { env ->
                            env.getArgument<List<String>>("ids")!!
                                .map(usersByIds::get)
                        }
                    }
            }
        ),
    ),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .deferSupport { true }
    }
}
