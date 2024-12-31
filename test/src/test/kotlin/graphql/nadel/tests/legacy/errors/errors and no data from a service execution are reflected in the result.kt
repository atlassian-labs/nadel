package graphql.nadel.tests.legacy.errors

import graphql.execution.DataFetcherResult
import graphql.nadel.engine.util.toGraphQLError
import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `errors and no data from a service execution are reflected in the result` : NadelLegacyIntegrationTest(
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
                wiring.type("Query") { type ->
                    type.dataFetcher("hello") { env ->
                        DataFetcherResult
                            .newResult<Any>()
                            .data(null)
                            .errors(
                                listOf(
                                    toGraphQLError(
                                        mapOf(
                                            "message"
                                                to "Problem1",
                                        ),
                                    ),
                                    toGraphQLError(mapOf("message" to "Problem2")),
                                ),
                            ).build()
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
