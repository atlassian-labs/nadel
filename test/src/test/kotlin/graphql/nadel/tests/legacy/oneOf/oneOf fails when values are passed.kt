package graphql.nadel.tests.legacy.oneOf

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `oneOf fails when values are passed` : NadelLegacyIntegrationTest(
    query = """
        query myQuery {
          search(by: {name: "Figaro", id: "1001"})
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "MyService",
            overallSchema = """
                type Query {
                  search(by: SearchInput!): String
                }
                input SearchInput @oneOf {
                  name: String
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  search(by: SearchInput!): String
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
