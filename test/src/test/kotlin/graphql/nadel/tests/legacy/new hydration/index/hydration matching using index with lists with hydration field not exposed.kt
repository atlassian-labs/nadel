package graphql.nadel.tests.legacy.`new hydration`.index

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `hydration matching using index with lists with hydration field not exposed` : NadelLegacyIntegrationTest(
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
                  echo: String
                  usersByIssueIds(issueIds: [ID]): [[User]]
                }
                type User {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  echo: String
                  usersByIssueIds(issueIds: [ID]): [[User]]
                }
                type User {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("usersByIssueIds") { env ->
                        if (env.getArgument<Any?>("issueIds") == listOf("ISSUE-1", "ISSUE-2")) {
                            listOf(
                                listOf(UserService_User(name = "Name")),
                                listOf(
                                    UserService_User(name = "Name"),
                                    UserService_User(name = "Name 2"),
                                ),
                            )
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
                  authors: [User]
                  @hydrated(
                    service: "UserService"
                    field: "usersByIssueIds"
                    arguments: [{name: "issueIds" value: "${'$'}source.id"}]
                    indexed: true
                    batchSize: 5
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Issue {
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
        val id: String? = null,
    )
}
