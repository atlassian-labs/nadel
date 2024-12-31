package graphql.nadel.tests.legacy.`field removed`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `top level field is removed for a subscription` : NadelLegacyIntegrationTest(
    query = """
        subscription {
          onCommentUpdated(id: "C1") {
            id
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
                  commentById(id: ID): Comment
                }
                type Subscription {
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
}
