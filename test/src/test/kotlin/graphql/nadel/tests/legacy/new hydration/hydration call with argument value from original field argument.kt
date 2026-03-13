package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `hydration call with argument value from original field argument` : NadelLegacyIntegrationTest(
    query = """
        query {
          issues {
            id
            author(extraArg: "extraArg") {
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
                  usersByIds(extraArg: String, id: [ID]): [User]
                }
                type User {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  usersByIds(extraArg: String, id: [ID]): [User]
                }
                type User {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    val usersByIds = listOf(
                        UserService_User(id = "USER-1", name = "User 1"),
                    ).associateBy { it.id }

                    type.dataFetcher("usersByIds") { env ->
                        if (env.getArgument<Any?>("extraArg") == "extraArg") {
                            env.getArgument<List<String>>("id")?.map(usersByIds::get)
                        } else {
                            null
                        }
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
                  author(extraArg: String): User @hydrated(
                    field: "usersByIds"
                    arguments: [
                      {name: "extraArg" value: "${'$'}argument.extraArg"}
                      {name: "id" value: "${'$'}source.authorId"}
                    ]
                    identifiedBy: "id"
                    batchSize: 2
                  )
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
                        listOf(Issues_Issue(authorId = "USER-1", id = "ISSUE-1"))
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
