package graphql.nadel.tests.legacy.basic

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `query to one service with execution input passed down` : NadelLegacyIntegrationTest(
    query = """
        query OpName {
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
                        MyService_World(id = "3", name = "earth")
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
