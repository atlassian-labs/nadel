package graphql.nadel.tests.legacy.oneOf

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `oneOf fails when null value is passed` : NadelLegacyIntegrationTest(
    query = """
        query myQuery {
          search(by: {name: null})
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
                input SearchInput @oneOf {
                  name: String
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  search(by: SearchInput): String
                }
                input SearchInput @oneOf {
                  name: String
                  id: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
    ),
) {
    private data class MyService_SearchInput(
        val name: String? = null,
        val id: String? = null,
    )
}
