package graphql.nadel.tests.legacy.errors

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `exceptions in service execution result completable future in graphql errors` : NadelLegacyIntegrationTest(
    query = """
        query {
          hello {
            name
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
                  helloWithArgs(arg1: String! arg2: String): World
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
                  helloWithArgs(arg1: String!, arg2: String): World
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
