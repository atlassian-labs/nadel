package graphql.nadel.tests.legacy.`field removed`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `hidden namespaced hydration top level field is removed` : NadelLegacyIntegrationTest(
    query = """
        query {
          issueById(id: "C1") {
            id
            comment {
              id
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "IssueService",
            overallSchema = """
                directive @namespaced on FIELD_DEFINITION
                type Query {
                  issueById(id: ID): Issue @namespaced
                }
                type Issue {
                  id: ID
                  comment: Comment @hydrated(
                    field: "commentApi.commentById"
                    arguments: [
                      {name: "id", value: "${'$'}source.commentId"}
                    ]
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  issueById(id: ID): Issue
                }
                type Issue {
                  id: ID
                  commentId: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issueById") { env ->
                        if (env.getArgument<Any?>("id") == "C1") {
                            IssueService_Issue(commentId = "C1", id = "C1")
                        } else {
                            null
                        }
                    }
                }
            },
        ),
        Service(
            name = "CommentService",
            overallSchema = """
                directive @toBeDeleted on FIELD_DEFINITION
                type Query {
                  commentApi: CommentApi @namespaced @hidden
                  echo: String
                }
                type CommentApi {
                  commentById(id: ID): Comment @toBeDeleted @hidden
                  echo: String
                }
                type Comment {
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  commentApi: CommentApi
                  echo: String
                }
                type CommentApi {
                  commentById(id: ID): Comment
                  echo: String
                }
                type Comment {
                  id: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
    ),
) {
    private data class IssueService_Issue(
        val id: String? = null,
        val commentId: String? = null,
    )

    private data class CommentService_Comment(
        val id: String? = null,
    )

    private data class CommentService_CommentApi(
        val commentById: CommentService_Comment? = null,
        val echo: String? = null,
    )
}
