package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `top level field data returns null in batched synthetic hydration` : NadelLegacyIntegrationTest(
    query = """
        query {
          issues {
            id
            authors {
              id
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "service2",
            overallSchema = """
                type Query {
                  users: UsersQuery
                }
                type UsersQuery {
                  usersByIds(id: [ID]): [User]
                }
                type User {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  users: UsersQuery
                }
                type User {
                  id: ID
                  name: String
                }
                type UsersQuery {
                  usersByIds(id: [ID]): [User]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("users") {
                        Unit
                    }
                }
                wiring.type("UsersQuery") { type ->
                    type.dataFetcher("usersByIds") { env ->
                        null
                    }
                }
            },
        ),
        Service(
            name = "service1",
            overallSchema = """
                type Query {
                  issues: [Issue]
                }
                type Issue {
                  id: ID
                  authors: [User]
                  @hydrated(
                    service: "service2"
                    field: "users.usersByIds"
                    arguments: [{name: "id" value: "${'$'}source.authorIds"}]
                    identifiedBy: "id"
                    batchSize: 3
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Issue {
                  authorIds: [ID]
                  id: ID
                }
                type Query {
                  issues: [Issue]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issues") { env ->
                        listOf(
                            Service1_Issue(authorIds = listOf("USER-1", "USER-2"), id = "ISSUE-1"),
                            Service1_Issue(authorIds = listOf("USER-3"), id = "ISSUE-2"),
                            Service1_Issue(
                                authorIds =
                                listOf("USER-2", "USER-4", "USER-5"),
                                id = "ISSUE-3",
                            ),
                        )
                    }
                }
            },
        ),
    ),
) {
    private data class Service2_User(
        val id: String? = null,
        val name: String? = null,
    )

    private data class Service2_UsersQuery(
        val usersByIds: List<Service2_User?>? = null,
    )

    private data class Service1_Issue(
        val authorIds: List<String?>? = null,
        val id: String? = null,
    )
}
