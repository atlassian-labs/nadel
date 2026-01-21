package graphql.nadel.tests.next.fixtures.hydration

import graphql.nadel.tests.next.NadelIntegrationTest

/**
 * Tests no calls made to `userById` field if `Issue.userId` is null.
 */
class HydrationNullSourceFieldWithNotNullArgumentValueTest : NadelIntegrationTest(
    query = """
        query {
          issue {
            user(referer: "echo") {
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
                  userById(id: ID!, referer: String): User
                }
                type Issue {
                  userId: ID
                  user(referer: String): User
                    @hydrated(
                      field: "userById"
                      arguments: [{name: "id", value: "$source.userId"}, {name: "referer", value: "$argument.referer"}]
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
