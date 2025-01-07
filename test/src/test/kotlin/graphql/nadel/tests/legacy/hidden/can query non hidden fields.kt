package graphql.nadel.tests.legacy.hidden

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `can query non hidden fields` : NadelLegacyIntegrationTest(
    query = """
        query {
          hello {
            id
            name
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "MyService",
            overallSchema = """
                directive @hidden on FIELD_DEFINITION
                type Query {
                  hello: World
                  hiddenField: String @hidden
                }
                type World {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  hello: World
                  hiddenField: String
                }
                type World {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("hello") { env ->
                        MyService_World(id = "ID", name = "World")
                    }
                }
            },
        ),
    ),
) {
    private data class MyService_World(
        val id: String? = null,
        val name: String? = null,
    )
}
