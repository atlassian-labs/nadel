package graphql.nadel.tests.legacy.introspection

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `introspection with variables FF on` : NadelLegacyIntegrationTest(
    query = """
        query {
          __schema {
            queryType {
              fields(includeDeprecated: true) {
                name
                isDeprecated
              }
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "MyService",
            overallSchema = """
                type Query {
                  earth: Planet
                  pluto: Planet @deprecated(reason: "Oh no")
                }
                type Planet {
                  id: ID
                }
                type Mutation {
                  hello: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Mutation {
                  hello: String
                }
                type Query {
                  earth: Planet
                  pluto: Planet @deprecated(reason: "Oh no")
                }
                type Planet {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
    ),
) {
    private data class MyService_Planet(
        val id: String? = null,
        val name: String? = null,
    )
}
