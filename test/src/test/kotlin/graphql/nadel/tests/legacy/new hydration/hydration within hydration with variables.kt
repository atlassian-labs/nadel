package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `hydration within hydration with variables` : NadelLegacyIntegrationTest(
    query = """
        query(${'$'}first: Int) {
          board {
            issue {
              comments(first: ${'$'}first) {
                totalCount
              }
            }
          }
        }
    """.trimIndent(),
    variables = mapOf("first" to 10),
    services = listOf(
        Service(
            name = "boards",
            overallSchema = """
                type Query {
                  board: Board
                }
                type Board {
                  id: ID
                  title: String
                  issue: Issue
                  @hydrated(
                    field: "issue"
                    arguments: [{name: "id" value: "${'$'}source.issueId"}]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  board: Board
                }
                type Board {
                  id: ID
                  title: String
                  issueId: ID!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("board") { env ->
                        Boards_Board(issueId = "ISSUE-1")
                    }
                }
            },
        ),
        Service(
            name = "issues",
            overallSchema = """
                type Query {
                  issue(id: ID!): Issue
                }
                type Issue {
                  id: ID!
                  cloudId: ID!
                  comments(first: Int = 20): CommentConnection
                  @hydrated(
                    field: "comments"
                    arguments: [
                      {name: "cloudId" value: "${'$'}source.cloudId"}
                      {name: "first" value: "${'$'}argument.first"}
                    ]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  issue(id: ID!): Issue
                }
                type Issue {
                  id: ID!
                  cloudId: ID!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issue") { env ->
                        if (env.getArgument<Any?>("id") == "ISSUE-1") {
                            Issues_Issue(cloudId = "CLOUD_ID-1")
                        } else {
                            null
                        }
                    }
                }
            },
        ),
        Service(
            name = "comments",
            overallSchema = """
                type Query {
                  comments(cloudId: ID!, first: Int): CommentConnection
                }
                type CommentConnection {
                  totalCount: Int
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  comments(cloudId: ID!, first: Int): CommentConnection
                }
                type CommentConnection {
                  totalCount: Int
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("comments") { env ->
                        if (env.getArgument<Any?>("cloudId") == "CLOUD_ID-1" &&
                            env.getArgument<Any?>("first") ==
                            10
                        ) {
                            Comments_CommentConnection(totalCount = 10)
                        } else {
                            null
                        }
                    }
                }
            },
        ),
    ),
) {
    private data class Boards_Board(
        val id: String? = null,
        val title: String? = null,
        val issueId: String? = null,
    )

    private data class Issues_Issue(
        val id: String? = null,
        val cloudId: String? = null,
    )

    private data class Comments_CommentConnection(
        val totalCount: Int? = null,
    )
}
