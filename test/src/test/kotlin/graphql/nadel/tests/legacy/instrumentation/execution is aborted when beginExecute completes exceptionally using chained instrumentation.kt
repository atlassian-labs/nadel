package graphql.nadel.tests.legacy.instrumentation

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `execution is aborted when beginExecute completes exceptionally using chained instrumentation` :
    NadelLegacyIntegrationTest(
        query = """
            query OpName {
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
                    }
                    type World {
                      id: ID
                      name: String
                    }
                """.trimIndent(),
                underlyingSchema = """
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
