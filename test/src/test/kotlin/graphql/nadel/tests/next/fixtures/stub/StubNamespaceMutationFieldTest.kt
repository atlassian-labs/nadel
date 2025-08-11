package graphql.nadel.tests.next.fixtures.stub

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

class StubNamespaceMutationFieldTest : NadelIntegrationTest(
    query = """
        mutation {
          jira {
            createLlmBackedIssue(input: {prompt: "Need tests for stubbed fields in Mutation context"}) {
              success
              issue {
                id
                key
                title
              }
            }
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
                type Mutation {
                  jira: JiraMutation @namespaced
                }
                type JiraMutation {
                  debug: String
                  createLlmBackedIssue(input: CreateLlmBackedIssueInput): CreateLlmBackedIssuePayload @stubbed
                }
                type CreateLlmBackedIssuePayload @stubbed {
                  success: Boolean
                  issue: Issue
                }
                input CreateLlmBackedIssueInput @stubbed {
                  prompt: String
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
                type Mutation {
                  jira: JiraMutation
                }
                type JiraMutation {
                  debug: String
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

                data class CreateLlmBackedIssuePayload(
                    val success: Boolean,
                    val issue: Issue,
                )

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("issue") { env ->
                                Issue(
                                    id = "123",
                                    title = "Wow an issue",
                                    description = "Stop procrastinating and do the work",
                                )
                            }
                    }
                    .type("Mutation") { type ->
                        type
                            .dataFetcher("createLlmBackedIssue") { env ->
                                throw UnsupportedOperationException("not implemented")
                            }
                    }
            },
        ),
    ),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .shortCircuitEmptyQuery { true }
    }
}
