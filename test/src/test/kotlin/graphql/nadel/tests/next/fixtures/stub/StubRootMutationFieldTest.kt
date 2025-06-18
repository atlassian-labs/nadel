package graphql.nadel.tests.next.fixtures.stub

import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * `Mutation.createLlmBackedIssue` is the stubbed field.
 *
 * Hmm, this isn't great because you can't apply `@stubbed` to input objects.
 */
class StubRootMutationFieldTest : NadelIntegrationTest(
    query = """
        mutation {
          createLlmBackedIssue(input: {prompt: "Need tests for stubbed fields in Mutation context"}) {
            success
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
                  _stub: String
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
                                Issue(
                                    id = "123",
                                    title = "Wow an issue",
                                    description = "Stop procrastinating and do the work",
                                )
                            }
                    }
            },
        ),
    ),
)
