package graphql.nadel.tests.legacy.`field removed`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `hydration top level field is removed` : NadelLegacyIntegrationTest(
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
                type Query {
                  issueById(id: ID): Issue
                }
                type Issue {
                  id: ID
                  comment: Comment @hydrated(
                    field: "commentById"
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
                  commentById(id: ID): Comment @toBeDeleted
                }
                type Comment {
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  commentById(id: ID): Comment
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
}
