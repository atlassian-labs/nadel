package graphql.nadel.tests.next.fixtures.stub

import graphql.nadel.tests.next.NadelIntegrationTest
import java.lang.UnsupportedOperationException

class StubEnumInputFieldTest : NadelIntegrationTest(
    query = """
        {
          thing(
            filter: {
                thing: THING1
            }
          )
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "myService",
            overallSchema = """
                type Query {
                    echo: String
                    thing(filter: ThingFilter): String @stubbed
                }
                input ThingFilter @stubbed {
                    thing: Thing
                }
                enum Thing @stubbed {
                    THING1
                    THING2
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  echo: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Issue(
                    val id: String,
                    val title: String,
                    val description: String,
                )
                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("thing") { env ->
                                throw UnsupportedOperationException()
                            }
                    }
            },
        ),
    ),
)
