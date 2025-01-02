package graphql.nadel.tests.legacy.introspection

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `no introspections on subscriptions` : NadelLegacyIntegrationTest(
    query = """
        subscription {
          __typename
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "MyService",
            overallSchema = """
                type Query {
                  comment: Comment
                }
                type Subscription {
                  onComment: Comment @namespaced
                }
                type Comment {
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  comment: Comment
                }
                type Subscription {
                  onComment: Comment
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
    private data class MyService_Comment(
        val id: String? = null,
    )
}
