package graphql.nadel.tests.legacy.graphqlcontext

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class graphqlcontextispresentoninput : NadelLegacyIntegrationTest(
    query = """
        query {
          hello(arg: "x") {
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
                  hello(arg: String): World
                }
                type World {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  hello(arg: String): World
                }
                type World {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("hello") { env ->
                        if (env.getArgument<Any?>("arg") == "x") {
                            null
                        } else {
                            null
                        }
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
