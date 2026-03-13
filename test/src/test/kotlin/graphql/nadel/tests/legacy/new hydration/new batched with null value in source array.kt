package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `new batched with null value in source array` : NadelLegacyIntegrationTest(
    query = """
        query {
          issueById(id: "10000") {
            key
            collaborators {
              __typename
              name
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "Issues",
            overallSchema = """
                type Query {
                  issueById(id: ID!): Issue!
                }
                type Issue {
                  id: ID!
                  key: String
                  collaborators: [User]
                  @hydrated(
                    field: "usersByIds"
                    arguments: [
                      {name: "ids", value: "${'$'}source.collaboratorIds"}
                    ]
                    identifiedBy: "id"
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  issueById(id: ID!): Issue!
                }
                type Issue {
                  id: ID!
                  key: String
                  collaboratorIds: [ID]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issueById") { env ->
                        if (env.getArgument<Any?>("id") == "10000") {
                            Issues_Issue(collaboratorIds = listOf("100", null, "200"), key = "GQLGW-1000")
                        } else {
                            null
                        }
                    }
                }
            },
        ),
        Service(
            name = "Users",
            overallSchema = """
                type Query {
                  usersByIds(ids: [ID!]!): [User]
                }
                type User {
                  id: ID!
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  usersByIds(ids: [ID!]!): [User]
                }
                type User {
                  id: ID!
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    val usersByIds = listOf(
                        Users_User(id = "100", name = "John Doe"),
                        Users_User(id = "200", name = "Joe")
                    ).associateBy { it.id }

                    type.dataFetcher("usersByIds") { env ->
                        env.getArgument<List<String>>("ids")?.map(usersByIds::get)
                    }
                }
            },
        ),
    ),
) {
    private data class Issues_Issue(
        val id: String? = null,
        val key: String? = null,
        val collaboratorIds: List<String?>? = null,
    )

    private data class Users_User(
        val id: String? = null,
        val name: String? = null,
    )
}
