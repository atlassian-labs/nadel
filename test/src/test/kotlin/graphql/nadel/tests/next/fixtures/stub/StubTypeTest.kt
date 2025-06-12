package graphql.nadel.tests.next.fixtures.stub

import graphql.nadel.tests.next.NadelIntegrationTest

class StubTypeTest : NadelIntegrationTest(
    query = """
        {
          person {
            address {
              __typename
              street
            }
          }
        }
    """.trimIndent(),
    services = listOf(
        Service(
            name = "myService",
            overallSchema = """
                type Query {
                  person: Person
                }
                type Person {
                  name: String
                  address: Address
                }
                type Address @stubbed {
                  street: String
                  postcode: Int
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  person: Person
                }
                type Person {
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                data class Person(
                    val name: String,
                )

                wiring
                    .type("Query") { type ->
                        type
                            .dataFetcher("person") { env ->
                                Person(name = "Fred")
                            }
                    }
            },
        ),
    ),
)
