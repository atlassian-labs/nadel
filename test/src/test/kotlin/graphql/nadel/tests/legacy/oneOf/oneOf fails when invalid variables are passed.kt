package graphql.nadel.tests.legacy.oneOf

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `oneOf fails when invalid variables are passed` : NadelLegacyIntegrationTest(
    query = """
        query myQuery(${'$'}name: String, ${'$'}id: ID) {
          search(by: {name: ${'$'}name, id: ${'$'}id})
        }
    """.trimIndent(),
    variables = mapOf("name" to "Figaro"),
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
