package graphql.nadel.tests.legacy.hidden

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `cannot query hidden top level fields` : NadelLegacyIntegrationTest(
    query = """
        query {
          hiddenField
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
            },
        ),
    ),
) {
    private data class MyService_World(
        val id: String? = null,
        val name: String? = null,
    )
}
