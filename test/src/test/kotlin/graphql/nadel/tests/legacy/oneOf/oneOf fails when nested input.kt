package graphql.nadel.tests.legacy.oneOf

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `oneOf fails when nested input` : NadelLegacyIntegrationTest(
    query = """
        query myQuery {
          search(by: {id: {email: null}})
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "MyService",
            overallSchema = """
                type Query {
                  search(by: SearchInput): String
                }
                input SearchInput {
                  name: String
                  id: IdInput
                }
                input IdInput @oneOf {
                  email: String
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  search(by: SearchInput): String
                }
                input SearchInput {
                  name: String
                  id: IdInput
                }
                input IdInput @oneOf {
                  email: String
                  id: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
    ),
) {
    private data class MyService_IdInput(
        val email: String? = null,
        val id: String? = null,
    )

    private data class MyService_SearchInput(
        val name: String? = null,
        val id: MyService_IdInput? = null,
    )
}
