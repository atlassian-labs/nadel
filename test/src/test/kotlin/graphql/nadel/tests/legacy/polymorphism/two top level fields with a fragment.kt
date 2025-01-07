package graphql.nadel.tests.legacy.polymorphism

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `two top level fields with a fragment` : NadelLegacyIntegrationTest(
    query = """
        fragment I on Issue {
          id
        }
        fragment U on User {
          id
          name
        }
        query {
          issues {
            ...I
          }
          user {
            ...U
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "UserService",
            overallSchema = """
                type Query {
                  user: User
                }
                type User {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  user: User
                }
                type User {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("user") { env ->
                        UserService_User(id = "USER-1", name = "User 1")
                    }
                }
            },
        ),
        Service(
            name = "Issues",
            overallSchema = """
                type Query {
                  issues: [Issue]
                }
                type Issue {
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Issue {
                  authorId: ID
                  id: ID
                }
                type Query {
                  issues: [Issue]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issues") { env ->
                        listOf(Issues_Issue(id = "ISSUE-1"), Issues_Issue(id = "ISSUE-2"))
                    }
                }
            },
        ),
    ),
) {
    private data class UserService_User(
        val id: String? = null,
        val name: String? = null,
    )

    private data class Issues_Issue(
        val authorId: String? = null,
        val id: String? = null,
    )
}
