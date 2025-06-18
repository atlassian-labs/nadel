package graphql.nadel.tests.next.fixtures.stub

import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * `Issue.key` is the stubbed field.
 */
class StubFieldInMutationOperationTest : NadelIntegrationTest(
    query = """
        mutation {
          createLlmBackedIssue(input: {prompt: "Need tests for stubbed fields in Mutation context"}) {
            success
            issue {
              id
              key
              title
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
                  createLlmBackedIssue(input: CreateLlmBackedIssueInput): CreateLlmBackedIssuePayload
                }
                type CreateLlmBackedIssuePayload {
                  success: Boolean
                  issue: Issue
                }
                input CreateLlmBackedIssueInput {
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
                  createLlmBackedIssue(input: CreateLlmBackedIssueInput): CreateLlmBackedIssuePayload
                }
                type CreateLlmBackedIssuePayload {
                  success: Boolean
                  issue: Issue
                }
                input CreateLlmBackedIssueInput {
                  prompt: String
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
                                CreateLlmBackedIssuePayload(
                                    success = true,
                                    issue = Issue(
                                        id = "123",
                                        title = "Wow an issue",
                                        description = "Stop procrastinating and do the work",
                                    ),
                                )
                            }
                    }
            },
        ),
    ),
)
