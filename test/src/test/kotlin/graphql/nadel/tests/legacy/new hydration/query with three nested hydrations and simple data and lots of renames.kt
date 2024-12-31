package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `query with three nested hydrations and simple data and lots of renames` : NadelLegacyIntegrationTest(
    query = """
        query {
          fooz {
            fooDetails {
              fooName
              fooAge
              fooContact {
                fooEmail
                fooPhone
              }
            }
            bar {
              barName
              nestedBar {
                barName
                nestedBar {
                  barName
                  barDetails {
                    barAge
                    barContact {
                      barEmail
                      barPhone
                    }
                  }
                }
              }
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "Bar",
            overallSchema = """
                type Query {
                  ibar: Bar @renamed(from: "bar")
                  barsById(id: [ID]): [Bar]
                }
                type Bar {
                  barId: ID
                  barName: String @renamed(from: "name")
                  nestedBar: Bar
                  @hydrated(
                    service: "Bar"
                    field: "barsById"
                    arguments: [{name: "id" value: "${'$'}source.nestedBarId"}]
                    identifiedBy: "barId"
                  )
                  barDetails: BarDetails @renamed(from: "details")
                }
                type BarDetails @renamed(from: "Details") {
                  barAge: Int @renamed(from: "age")
                  barContact: BarContactDetails @renamed(from: "contact")
                }
                type BarContactDetails @renamed(from: "ContactDetails") {
                  barEmail: String @renamed(from: "email")
                  barPhone: Int @renamed(from: "phone")
                }
            """.trimIndent(),
            underlyingSchema = """
                type Bar {
                  barId: ID
                  details: Details
                  name: String
                  nestedBarId: ID
                }
                type ContactDetails {
                  email: String
                  phone: Int
                }
                type Details {
                  age: Int
                  contact: ContactDetails
                }
                type Query {
                  bar: Bar
                  barsById(id: [ID]): [Bar]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("barsById") { env ->
                        if (env.getArgument<Any?>("id") == listOf("bar1")) {
                            listOf(Bar_Bar(barId = "bar1", name = "Bar 1", nestedBarId = "nestedBar1"))
                        } else if (env.getArgument<Any?>("id") == listOf("nestedBar1")) {
                            listOf(
                                Bar_Bar(
                                    barId = "nestedBar1",
                                    name = "NestedBarName1",
                                    nestedBarId =
                                    "nestedBarId456",
                                ),
                            )
                        } else if (env.getArgument<Any?>("id") == listOf("nestedBarId456")) {
                            listOf(
                                Bar_Bar(
                                    barId = "nestedBarId456",
                                    details =
                                    Bar_Details(
                                        age = 1,
                                        contact =
                                        Bar_ContactDetails(email = "test", phone = 1),
                                    ),
                                    name = "NestedBarName2",
                                ),
                            )
                        } else {
                            null
                        }
                    }
                }
            },
        ),
        Service(
            name = "Foo",
            overallSchema = """
                type Query {
                  fooz: [Fooz] @renamed(from: "foos")
                }
                type Fooz @renamed(from: "Foo") {
                  fooDetails: FooDetails @renamed(from: "details")
                  bar: Bar
                  @hydrated(
                    service: "Bar"
                    field: "barsById"
                    arguments: [{name: "id" value: "${'$'}source.barId"}]
                    identifiedBy: "barId"
                    batchSize: 2
                  )
                }
                type FooDetails @renamed(from: "Details") {
                  fooName: String @renamed(from: "name")
                  fooAge: Int @renamed(from: "age")
                  fooContact: FooContactDetails @renamed(from: "contact")
                }
                type FooContactDetails @renamed(from: "ContactDetails") {
                  fooEmail: String @renamed(from: "email")
                  fooPhone: Int @renamed(from: "phone")
                }
            """.trimIndent(),
            underlyingSchema = """
                type ContactDetails {
                  email: String
                  phone: Int
                }
                type Details {
                  age: Int
                  contact: ContactDetails
                  name: String
                }
                type Foo {
                  barId: ID
                  details: Details
                }
                type Query {
                  foos: [Foo]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foos") { env ->
                        listOf(
                            Foo_Foo(
                                barId = "bar1",
                                details =
                                Foo_Details(
                                    age = 1,
                                    contact =
                                    Foo_ContactDetails(email = "test", phone = 1),
                                    name = "smith",
                                ),
                            ),
                        )
                    }
                }
            },
        ),
    ),
) {
    private data class Bar_Bar(
        val barId: String? = null,
        val details: Bar_Details? = null,
        val name: String? = null,
        val nestedBarId: String? = null,
    )

    private data class Bar_ContactDetails(
        val email: String? = null,
        val phone: Int? = null,
    )

    private data class Bar_Details(
        val age: Int? = null,
        val contact: Bar_ContactDetails? = null,
    )

    private data class Foo_ContactDetails(
        val email: String? = null,
        val phone: Int? = null,
    )

    private data class Foo_Details(
        val age: Int? = null,
        val contact: Foo_ContactDetails? = null,
        val name: String? = null,
    )

    private data class Foo_Foo(
        val barId: String? = null,
        val details: Foo_Details? = null,
    )
}
