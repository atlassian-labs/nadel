package graphql.nadel.tests.legacy.basic

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `mutation can be executed` : NadelLegacyIntegrationTest(
    query = """
        mutation M {
          hello
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
                wiring.type("Mutation") { type ->
                    type.dataFetcher("hello") { env ->
                        "world"
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
