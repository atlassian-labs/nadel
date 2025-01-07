package graphql.nadel.tests.legacy.introspection

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `can specify max introspection response size` : NadelLegacyIntegrationTest(
    query = """
        query Test {
          __schema {
            types {
              name
              description
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
                  hello: World
                }
                type World {
                  id: ID
                  name: String
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
                  hello: World
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
