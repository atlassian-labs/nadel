package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * Tests no calls made to `userById` field if `Issue.userId` is null.
 */
class HydrationNullSourceFieldValueTest : NadelIntegrationTest(
    query = """
        query {
          issue {
            user {
              id
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "monolith",
            overallSchema = """
                type Query {
                  issue: Issue
                  userById(id: ID!): User
                }
                type Issue {
                  userId: ID
                  user: User
                    @hydrated(
                      field: "userById"
                      arguments: [{name: "id", value: "$source.userId"}]
                    )
                }
                type User {
                  id: ID!
                }
            """.trimIndent(),
            runtimeWiring = { runtime ->
                data class Issue(val userId: String?)
                data class User(val id: String)
                runtime
                    .type("Query") { type ->
                        type
                            .dataFetcher("issue") { env ->
                                Issue(userId = null)
                            }
                            .dataFetcher("userById") { env ->
                                User(id = env.getArgument<String>("id")!!)
                            }
                    }
            },
        ),
    ),
)
