package graphql.nadel.tests.legacy.`execution id`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `execution id is transferred from provider if missing in input` : NadelLegacyIntegrationTest(
    query = """
        query {
          hello {
            name
          }
          hello {
            id
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
                type Subscription {
                  onWorldUpdate: World
                  onAnotherUpdate: World
                }
            """.trimIndent(),
            underlyingSchema = """
                type Mutation {
                  hello: String
                }
                type Query {
                  hello: World
                }
                type Subscription {
                  onAnotherUpdate: World
                  onWorldUpdate: World
                }
                type World {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("hello") { env ->
                        null
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
