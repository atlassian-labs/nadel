package graphql.nadel.tests.next.fixtures.stub

import graphql.nadel.tests.next.NadelIntegrationTest
import java.lang.UnsupportedOperationException

class StubEnumTypeTest : NadelIntegrationTest(
    query = """
        {
          thing
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "myService",
            overallSchema = """
                type Query {
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
