package graphql.nadel.tests.next.fixtures.hydration.statics

import graphql.nadel.NadelExecutionHints
import graphql.nadel.tests.next.NadelIntegrationTest

class StaticHydrationScalarFieldTest : NadelIntegrationTest(
    query = """
        query {
          copyField(id: "wow")
        }
    """.trimIndent(),
    services = listOf(
        // Backing service
        Service(
            name = "graph_store",
            overallSchema = """
                type Query {
                  backingField(
                    id: String
                    secret: String
                  ): String @hidden
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("backingField") { env ->
                                env.getArgument<String>("id")
                            }
                    }
            },
        ),
        // Service that introduces virtual type
        Service(
            name = "work",
            overallSchema = """
                type Query {
                  copyField(
                    id: ID!
                  ): String
                    @hydrated(
                      service: "graph_store",
                      field: "backingField"
                      arguments: [
                        {
                          name: "id"
                          value: "$argument.id"
                        }
                        {
                          name: "secret"
                          value: "cowabunga"
                        }
                      ]
                    )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  business_stub: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
    ),
) {
    override fun makeExecutionHints(): NadelExecutionHints.Builder {
        return super.makeExecutionHints()
            .virtualTypeSupport { true }
            .shortCircuitEmptyQuery { true }
    }
}
