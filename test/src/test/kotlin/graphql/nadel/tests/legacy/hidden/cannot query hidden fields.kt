package graphql.nadel.tests.legacy.hidden

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `cannot query hidden fields` : NadelLegacyIntegrationTest(
    query = """
        query {
          hello {
            id
            name
            area51 {
              name
              coordinates
            }
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
                }
                type World {
                  id: ID
                  name: String
                  area51: Area @hidden
                }
                type Area {
                  name: String
                  coordinates: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  hello: World
                }
                type World {
                  id: ID
                  name: String
                  area51: Area
                }
                type Area {
                  name: String
                  coordinates: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
    ),
) {
    private data class MyService_Area(
        val name: String? = null,
        val coordinates: String? = null,
    )

    private data class MyService_World(
        val id: String? = null,
        val name: String? = null,
        val area51: MyService_Area? = null,
    )
}
