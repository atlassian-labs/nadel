package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `hydration from field in interface` : NadelLegacyIntegrationTest(
    query = """
        query {
          issue(id: "1") {
            title
            issueAuthor {
              name
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "issues",
            overallSchema = """
                type Query {
                  issue(id: ID): Issue
                }
                type Issue {
                  id: ID
                  title: String
                  issueAuthor: User
                  @hydrated(
                    service: "users"
                    field: "user"
                    arguments: [{name: "id" value: "${'$'}source.author.userId"}]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Issue {
                  id: ID!
                  title: String
                  author: User
                }
                interface User {
                  userId: ID!
                }
                type ProductUser implements User {
                  userId: ID!
                  email: String
                }
                type Query {
                  issue(id: ID): Issue
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issue") { env ->
                        if (env.getArgument<Any?>("id") == "1") {
                            Issues_Issue(author = Issues_ProductUser(userId = "1001"), title = "Issue 1")
                        } else {
                            null
                        }
                    }
                }
                wiring.type("User") { type ->
                    type.typeResolver { typeResolver ->
                        val obj = typeResolver.getObject<Any>()
                        val typeName = obj.javaClass.simpleName.substringAfter("_")
                        typeResolver.schema.getTypeAs(typeName)
                    }
                }
            },
        ),
        Service(
            name = "users",
            overallSchema = """
                type Query {
                  user(id: ID!): User
                }
                type User {
                  id: ID!
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type User {
                  id: ID!
                  name: String
                }
                type Query {
                  user(id: ID!): User
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("user") { env ->
                        if (env.getArgument<Any?>("id") == "1001") {
                            Users_User(name = "McUser Face")
                        } else {
                            null
                        }
                    }
                }
            },
        ),
    ),
) {
    private data class Issues_Issue(
        val id: String? = null,
        val title: String? = null,
        val author: Issues_User? = null,
    )

    private data class Issues_ProductUser(
        override val userId: String? = null,
        val email: String? = null,
    ) : Issues_User

    private interface Issues_User {
        val userId: String?
    }

    private data class Users_User(
        val id: String? = null,
        val name: String? = null,
    )
}
