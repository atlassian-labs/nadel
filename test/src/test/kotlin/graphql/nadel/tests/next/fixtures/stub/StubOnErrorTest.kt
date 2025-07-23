package graphql.nadel.tests.next.fixtures.stub

import graphql.ErrorClassification
import graphql.execution.DataFetcherResult
import graphql.nadel.tests.next.NadelIntegrationTest
import graphql.nadel.util.GraphQLErrorBuilder

class StubOnErrorTest : NadelIntegrationTest(
    query = """
        {
          issue {
            id
            key
            description
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "myService",
            overallSchema = """
                type Query {
                  issue: Issue
                }
                type Issue {
                  id: ID!
                  key: String @stubbed
                  title: String
                  description: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  issue: Issue
                }
                type Issue {
                  id: ID!
                  title: String
                  description: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(
                    val id: String,
                    val title: String,
                    val description: String,
                )
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("issue") { env ->
                                DataFetcherResult.newResult<Any?>()
                                    .data(null)
                                    .error(
                                        GraphQLErrorBuilder.newError()
                                            .message("Something broke")
                                            .errorType(ErrorClassification.errorClassification("Sabotage"))
                                            .build()
                                    )
                                    .build()
                            }
                    }
            },
        ),
    ),
)
