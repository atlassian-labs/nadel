package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `hydration input is absent` : NadelLegacyIntegrationTest(
    query = """
        query {
          issues {
            id
            authors {
              name
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "UserService",
            overallSchema = """
                type Query {
                  userById(id: ID!): User
                }
                type User {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  userById(id: ID!): User
                }
                type User {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
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
                  authors: [User] @hydrated(
                    field: "userById"
                    arguments: [
                      {name: "id" value: "${'$'}source.authors.id"}
                    ]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Issue {
                  id: ID!
                  authors: [AuthorRef]
                }
                type AuthorRef {
                  id: ID
                }
                type Query {
                  issues: [Issue]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issues") { env ->
                        listOf(Issues_Issue(authors = listOf(), id = "ISSUE-1"))
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

    private data class Issues_AuthorRef(
        val id: String? = null,
    )

    private data class Issues_Issue(
        val id: String? = null,
        val authors: List<Issues_AuthorRef?>? = null,
    )
}
