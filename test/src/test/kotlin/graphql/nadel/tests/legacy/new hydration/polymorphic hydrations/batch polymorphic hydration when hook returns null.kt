package graphql.nadel.tests.legacy.`new hydration`.`polymorphic hydrations`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `batch polymorphic hydration when hook returns null` : NadelLegacyIntegrationTest(
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
                  petById(ids: [ID]): [Pet]
                }
                type Pet {
                  id: ID
                  breed: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  petById(ids: [ID]): [Pet]
                }
                type Pet {
                  id: ID
                  breed: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    val petById = listOf(
                        Pets_Pet(breed = "Akita", id = "PET-0"),
                        Pets_Pet(breed = "Labrador", id = "PET-1"),
                    ).associateBy { it.id }

                    type
                        .dataFetcher("petById") { env ->
                            env.getArgument<List<String>>("ids")?.map(petById::get)
                        }
                }
            },
        ),
        Service(
            name = "people",
            overallSchema = """
                type Query {
                  humanById(ids: [ID]): [Human]
                }
                type Human {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  humanById(ids: [ID]): [Human]
                }
                type Human {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    val humansById = listOf(
                        People_Human(id = "HUMAN-0", name = "Fanny Longbottom"),
                        People_Human(id = "HUMAN-1", name = "John Doe")
                    ).associateBy { it.id }

                    type.dataFetcher("humanById") { env ->
                        env.getArgument<List<String>>("ids")?.map(humansById::get)
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
                    field: "petById"
                    arguments: [
                      {name: "ids" value: "${'$'}source.dataId"}
                    ]
                  )
                  @hydrated(
                    field: "humanById"
                    arguments: [
                      {name: "ids" value: "${'$'}source.dataId"}
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
                            Foo_Foo(dataId = "HUMAN-0", id = "FOO-1"),
                            Foo_Foo(dataId = "NULL-1", id = "FOO-2"),
                            Foo_Foo(dataId = "HUMAN-1", id = "FOO-3"),
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
