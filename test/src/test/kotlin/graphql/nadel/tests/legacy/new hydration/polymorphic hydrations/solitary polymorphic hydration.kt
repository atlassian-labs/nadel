package graphql.nadel.tests.legacy.`new hydration`.`polymorphic hydrations`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `solitary polymorphic hydration` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo {
            __typename
            id
            data {
              ... on Pet {
                __typename
                id
                breed
              }
              ... on Human {
                __typename
                id
                name
              }
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "pets",
            overallSchema = """
                type Query {
                  petById(id: ID): Pet
                }
                type Pet {
                  id: ID
                  breed: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  petById(id: ID): Pet
                }
                type Pet {
                  id: ID
                  breed: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("petById") { env ->
                        if (env.getArgument<Any?>("id") == "PET-0") {
                            Pets_Pet(breed = "Akita", id = "PET-0")
                        } else {
                            null
                        }
                    }
                }
            },
        ),
        Service(
            name = "people",
            overallSchema = """
                type Query {
                  humanById(id: ID): Human
                }
                type Human {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  humanById(id: ID): Human
                }
                type Human {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("humanById") { env ->
                        if (env.getArgument<Any?>("id") == "HUMAN-0") {
                            People_Human(id = "HUMAN-0", name = "Fanny Longbottom")
                        } else {
                            null
                        }
                    }
                }
            },
        ),
        Service(
            name = "foo",
            overallSchema = """
                type Query {
                  foo: [Foo]
                }
                type Foo {
                  id: ID
                  dataId: ID
                  data: Data
                  @hydrated(
                    service: "pets"
                    field: "petById"
                    arguments: [
                      {name: "id" value: "${'$'}source.dataId"}
                    ]
                  )
                  @hydrated(
                    service: "people"
                    field: "humanById"
                    arguments: [
                      {name: "id" value: "${'$'}source.dataId"}
                    ]
                  )
                }
                union Data = Pet | Human
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  foo: [Foo]
                }
                type Foo {
                  id: ID
                  dataId: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        listOf(
                            Foo_Foo(dataId = "PET-0", id = "FOO-0"),
                            Foo_Foo(
                                dataId = "HUMAN-0",
                                id =
                                "FOO-1",
                            ),
                        )
                    }
                }
            },
        ),
    ),
) {
    private data class Pets_Pet(
        val id: String? = null,
        val breed: String? = null,
    )

    private data class People_Human(
        val id: String? = null,
        val name: String? = null,
    )

    private data class Foo_Foo(
        val id: String? = null,
        val dataId: String? = null,
    )
}
