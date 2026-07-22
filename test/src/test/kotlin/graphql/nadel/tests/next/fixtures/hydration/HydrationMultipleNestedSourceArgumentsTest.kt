package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * Tests that we can query source fields like
 *
 * `$source.assigneeCriteria.siteId`
 * `$source.id`
 * `$source.assigneeCriteria.assigneeId`
 *
 * There was a bug in the past where only one field under `assigneeCriteria` was queried instead of two.
 */
class HydrationMultipleNestedSourceArgumentsTest : NadelIntegrationTest(
    query = """
        query {
          issueById(id: "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1") {
            id
            key
            assignee {
              id
              name
            }
          }
        }
    """.trimIndent(),
    variables = mapOf(),
    services = listOf(
        Service(
            name = "issues",
            overallSchema = """
                type Query {
                  issueById(id: ID!): Issue
                }
                type Issue {
                  id: ID!
                  key: String
                  assigneeCriteria: IssueUserCriteria @hidden
                  assignee: User
                    @hydrated(
                      field: "issueUser"
                      arguments: [
                        {name: "siteId", value: "$source.assigneeCriteria.siteId"}
                        {name: "issueId", value: "$source.id"}
                        {name: "userId", value: "$source.assigneeCriteria.assigneeId"}
                      ]
                    )
                }
                type IssueUserCriteria {
                  siteId: ID!
                  assigneeId: ID!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class IssueUserCriteria(
                    val siteId: String,
                    val assigneeId: String,
                )

                data class Issue(
                    val id: String,
                    val key: String,
                    val assigneeCriteria: IssueUserCriteria? = null,
                )

                val issuesById = listOf(
                    Issue(
                        id = "ari:cloud:jira:19b8272f-8d25-4706-adce-8db72305e615:issue/1",
                        key = "GQLGW-1",
                        assigneeCriteria = IssueUserCriteria(
                            "ari:cloud:platform::site/123",
                            "ari:cloud:identity::user/1",
                        ),
                    )
                ).associateBy { it.id }

                wiring
                    .type("Query") { type ->
                        type.dataFetcher("issueById") {
                            issuesById[it.getArgument("id")]
                        }
                    }
            },
        ),
        Service(
            name = "identity",
            overallSchema = """
                type Query {
                  issueUser(
                    siteId: ID!
                    issueId: ID!
                    userId: ID!
                  ): User
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

                wiring
                    .type("Query") { type ->
                        type.dataFetcher("issueUser") {
                            User(
                                id = "ari:cloud:identity::user/1",
                                name = "Franklin Wang",
                            )
                        }
                    }
            },
        ),
    ),
)
