package graphql.nadel.tests.legacy.hidden

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `introspection does not show hidden fields` : NadelLegacyIntegrationTest(
    query = """
        query introspection_query {
          __schema {
            queryType {
              fields(includeDeprecated: false) {
                name
              }
            }
          }
          __type(name: "World") {
            name
            fields {
              name
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
                  hiddenField: String @hidden
                }
                type World {
                  id: ID
                  name: String
                  area51: String @hidden
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
                  area51: String
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
        val area51: String? = null,
    )
}
