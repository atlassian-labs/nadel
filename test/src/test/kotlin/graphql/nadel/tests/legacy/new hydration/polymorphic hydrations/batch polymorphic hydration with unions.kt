package graphql.nadel.tests.legacy.`new hydration`.`polymorphic hydrations`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `batch polymorphic hydration with unions` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo {
            __typename
            id
            data {
              ... on Dog {
                __typename
                id
                breed
              }
              ... on Fish {
                __typename
                id
                fins
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
                type Fish {
                  id: ID
                  fins: Int
                }
                type Dog {
                  id: ID
                  breed: String
                }
                union Pet = Fish | Dog
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  petById(ids: [ID]): [Pet]
                }
                type Fish {
                  id: ID
                  fins: Int
                }
                type Dog {
                  id: ID
                  breed: String
                }
                union Pet = Fish | Dog
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    val petById = listOf(
                        Pets_Dog(id = "DOG-0", breed = "Akita"),
                        Pets_Fish(id = "FISH-0", fins = 4),
                        Pets_Dog(id = "DOG-1", breed = "Labrador"),
                        Pets_Fish(id = "FISH-1", fins = 8),
                    ).associateBy { it.id }

                    type
                        .dataFetcher("petById") { env ->
                            env.getArgument<List<String>>("ids")?.map(petById::get)
                        }
                }
                wiring.type("Pet") { type ->
                    type.typeResolver { typeResolver ->
                        val obj = typeResolver.getObject<Any>()
                        val typeName = obj.javaClass.simpleName.substringAfter("_")
                        typeResolver.schema.getTypeAs(typeName)
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
                    val humanById = listOf(
                        People_Human(id = "HUMAN-0", name = "Fanny Longbottom"),
                        People_Human(id = "HUMAN-1", name = "John Doe"),
                    ).associateBy { it.id }

                    type
                        .dataFetcher("humanById") { env ->
                            env.getArgument<List<String>>("ids")?.map(humanById::get)
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
                  data: LivingThing
                  @hydrated(
                    field: "petById"
                    arguments: [
                      {name: "ids" value: "${'$'}source.dataId"}
                    ]
                    identifiedBy: "id"
                  )
                  @hydrated(
                    field: "humanById"
                    arguments: [
                      {name: "ids" value: "${'$'}source.dataId"}
                    ]
                    identifiedBy: "id"
                  )
                }
                union LivingThing = Human | Fish | Dog
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
                            Foo_Foo(dataId = "DOG-0", id = "FOO-0"),
                            Foo_Foo(dataId = "FISH-0", id = "FOO-1"),
                            Foo_Foo(dataId = "DOG-1", id = "FOO-2"),
                            Foo_Foo(dataId = "FISH-1", id = "FOO-3"),
                            Foo_Foo(dataId = "HUMAN-0", id = "FOO-4"),
                        )
                    }
                }
            },
        ),
    ),
) {
    private data class Pets_Dog(
        override val id: String? = null,
        val breed: String? = null,
    ) : Pets_Pet

    private data class Pets_Fish(
        override val id: String? = null,
        val fins: Int? = null,
    ) : Pets_Pet

    private sealed interface Pets_Pet {
        val id: String?
    }

    private data class People_Human(
        val id: String? = null,
        val name: String? = null,
    )

    private data class Foo_Foo(
        val id: String? = null,
        val dataId: String? = null,
    )
}
