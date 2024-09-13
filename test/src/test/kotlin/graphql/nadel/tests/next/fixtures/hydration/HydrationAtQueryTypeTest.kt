package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.tests.next.NadelIntegrationTest

class HydrationAtQueryTypeTest : NadelIntegrationTest(
    query = """
        query {
          myIssue {
            title
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "issues",
            overallSchema = """
                type Query {
                  issueById(id: ID!): Issue
                  myIssueKey: ID! @hidden
                  myIssue: Issue
                    @hydrated(
                      service: "issues"
                      field: "issueById"
                      arguments: [{name: "id", value: "$source.myIssueKey"}]
                    )
                }
                type Issue {
                  id: ID!
                  title: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(
                    val id: String,
                    val title: String,
                )

                val issuesByIds = listOf(
                    Issue(id = "hello", title = "Hello there"),
                    Issue(id = "afternoon", title = "Good afternoon"),
                    Issue(id = "bye", title = "Farewell"),
                ).strictAssociateBy { it.id }

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("issueById") { env ->
                                issuesByIds[env.getArgument<String>("id")]
                            }
                            .dataFetcher("myIssueKey") { env ->
                                "bye"
                            }
                    }
            },
        ),
    ),
)
