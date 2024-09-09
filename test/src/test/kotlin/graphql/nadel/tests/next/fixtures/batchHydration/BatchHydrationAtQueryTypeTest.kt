package graphql.nadel.tests.next.fixtures.batchHydration

import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.tests.next.NadelIntegrationTest

class BatchHydrationAtQueryTypeTest : NadelIntegrationTest(
    query = """
        query {
          myIssues {
            title
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "issues",
            overallSchema = """
                type Query {
                  issuesByIds(ids: [ID!]!): [Issue]
                  myIssueKeys(limit: Int! = 25): [ID!] @hidden
                  myIssues: [Issue]
                    @hydrated(
                      service: "issues"
                      field: "issuesByIds"
                      arguments: [{name: "ids", value: "$source.myIssueKeys"}]
                      identifiedBy: "id"
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
                            .dataFetcher("issuesByIds") { env ->
                                env.getArgument<List<String>>("ids")
                                    ?.map {
                                        issuesByIds[it]
                                    }
                            }
                            .dataFetcher("myIssueKeys") { env ->
                                listOf("hello", "bye")
                            }
                    }
            },
        ),
    ),
)
