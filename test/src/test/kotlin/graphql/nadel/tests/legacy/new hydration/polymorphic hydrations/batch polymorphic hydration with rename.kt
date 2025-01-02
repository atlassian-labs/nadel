package graphql.nadel.tests.legacy.`new hydration`.`polymorphic hydrations`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `batch polymorphic hydration with rename` : NadelLegacyIntegrationTest(
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
                  breed: String @renamed(from: "kind")
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  petById(ids: [ID]): [Pet]
                }
                type Pet {
                  id: ID
                  kind: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    val petById = listOf(
                        Pets_Pet(id = "PET-0", kind = "Akita"),
                        Pets_Pet(id = "PET-1", kind = "Labrador"),
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
                type Human @renamed(from: "Person") {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  humanById(ids: [ID]): [Person]
                }
                type Person {
                  id: ID
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("humanById") { env ->
                        if (env.getArgument<Any?>("ids") == listOf("HUMAN-0", "HUMAN-1")) {
                            listOf(
                                People_Person(id = "HUMAN-0", name = "Fanny Longbottom"),
                                People_Person(
                                    id =
                                    "HUMAN-1",
                                    name = "John Doe",
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
                      {name: "ids" value: "${'$'}source.dataId"}
                    ]
                  )
                  @hydrated(
                    service: "people"
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
                            Foo_Foo(dataId = "PET-1", id = "FOO-2"),
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
        val kind: String? = null,
    )

    private data class People_Person(
        val id: String? = null,
        val name: String? = null,
    )

    private data class Foo_Foo(
        val id: String? = null,
        val dataId: String? = null,
    )
}
