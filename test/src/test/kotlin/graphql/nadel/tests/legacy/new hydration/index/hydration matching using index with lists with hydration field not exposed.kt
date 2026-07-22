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
                    val usersByIssueId = mapOf(
                        "ISSUE-1" to listOf(
                            UserService_User(id = "1", name = "Name"),
                        ),
                        "ISSUE-2" to listOf(
                            UserService_User(id = "1", name = "Name"),
                            UserService_User(id = "2", name = "Name 2"),
                        ),
                    )

                    type.dataFetcher("usersByIssueIds") { env ->
                        env.getArgument<List<String>>("issueIds")?.map(usersByIssueId::get)
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
