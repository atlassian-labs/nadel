package graphql.nadel.tests.legacy.`field removed`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `namespaced field is removed` : NadelLegacyIntegrationTest(
    query = """
        query {
          commentApi {
            commentById(id: "C1") {
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
                directive @namespaced on FIELD_DEFINITION
                type Query {
                  commentApi: CommentApi @namespaced
                }
                type CommentApi {
                  commentById(id: ID): Comment @toBeDeleted
                }
                type Comment {
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  commentApi: CommentApi
                }
                type CommentApi {
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
    private data class CommentService_Comment(
        val id: String? = null,
    )

    private data class CommentService_CommentApi(
        val commentById: CommentService_Comment? = null,
    )
}
