package graphql.nadel.tests.legacy.oneOf

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `oneOf successful` : NadelLegacyIntegrationTest(
    query = """
        query myQuery {
          search(by: {name: "Figaro"})
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
                wiring.type("Query") { type ->
                    type.dataFetcher("search") { env ->
                        if (env.getArgument<Any?>("by") == mapOf("name" to "Figaro")) {
                            "Figaro"
                        } else {
                            null
                        }
                    }
                }
            },
        ),
    ),
) {
    private data class MyService_SearchInput(
        val name: String? = null,
        val id: String? = null,
    )
}
