package graphql.nadel.tests.legacy.`field removed`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `renamed top level field is not removed short circuit hint is on` : NadelLegacyIntegrationTest(
    query = """
        query {
          commentById(id: "C1") {
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
                  commentById(id: ID): Comment @renamed(from: "commentByIdUnderlying")
                }
                type Comment {
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  commentByIdUnderlying(id: ID): Comment
                }
                type Comment {
                  id: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("commentByIdUnderlying") { env ->
                        if (env.getArgument<Any?>("id") == "C1") {
                            CommentService_Comment(id = "C1")
                        } else {
                            null
                        }
                    }
                }
            },
        ),
    ),
) {
    private data class CommentService_Comment(
        val id: String? = null,
    )
}
