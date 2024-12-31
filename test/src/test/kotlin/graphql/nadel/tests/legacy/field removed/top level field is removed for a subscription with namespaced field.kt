package graphql.nadel.tests.legacy.`field removed`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `top level field is removed for a subscription with namespaced field` : NadelLegacyIntegrationTest(
    query = """
        subscription {
          commentsApi {
            onCommentUpdated(id: "C1") {
              id
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "CommentService",
            overallSchema = """
                directive @toBeDeleted on FIELD_DEFINITION
                type Query {
                  commentById(id: ID): Comment @toBeDeleted
                }
                type Subscription {
                  commentsApi: CommentsApi @namespaced
                }
                type CommentsApi {
                  onCommentUpdated(id: ID): Comment @toBeDeleted
                }
                type Comment {
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  commentById(id: ID): Comment
                }
                type Subscription {
                  commentsApi: CommentsApi
                }
                type CommentsApi {
                  onCommentUpdated(id: ID): Comment
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
    private data class CommentService_Comment(
        val id: String? = null,
    )

    private data class CommentService_CommentsApi(
        val onCommentUpdated: CommentService_Comment? = null,
    )
}
