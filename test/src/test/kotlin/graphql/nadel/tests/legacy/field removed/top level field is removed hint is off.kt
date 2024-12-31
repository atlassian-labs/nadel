package graphql.nadel.tests.legacy.`field removed`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `top level field is removed hint is off` : NadelLegacyIntegrationTest(
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
                wiring.type("Query") { type ->
                    type.dataFetcher("__typename") { env ->
                        "Query"
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
