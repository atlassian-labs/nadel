package graphql.nadel.tests.legacy.`new hydration`.`polymorphic hydrations`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `batch polymorphic hydration where only one type is queried` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo {
            data {
              ... on Fish {
                __typename
                id
                fins
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
                    type.dataFetcher("petById") { env ->
                        if (env.getArgument<Any?>("ids") == listOf("DOG-0", "FISH-0", "DOG-1", "FISH-1")) {
                            listOf(
                                Pets_Dog(id = "DOG-0"),
                                Pets_Fish(id = "FISH-0", fins = 4),
                                Pets_Dog(
                                    id =
                                    "DOG-1",
                                ),
                                Pets_Fish(id = "FISH-1", fins = 8),
                            )
                        } else {
                            null
                        }
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
                    type.dataFetcher("humanById") { env ->
                        if (env.getArgument<Any?>("ids") == listOf("HUMAN-0")) {
                            listOf(People_Human(id = "HUMAN-0"))
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
                  data: LivingThing
                  @hydrated(
                    service: "pets"
                    field: "petById"
                    arguments: [
                      {name: "ids" value: "${'$'}source.dataId"}
                    ]
                    identifiedBy: "id"
                  )
                  @hydrated(
                    service: "people"
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
                            Foo_Foo(dataId = "DOG-0"),
                            Foo_Foo(dataId = "FISH-0"),
                            Foo_Foo(dataId = "DOG-1"),
                            Foo_Foo(dataId = "FISH-1"),
                            Foo_Foo(dataId = "HUMAN-0"),
                        )
                    }
                }
            },
        ),
    ),
) {
    private data class Pets_Dog(
        val id: String? = null,
        val breed: String? = null,
    ) : Pets_Pet

    private data class Pets_Fish(
        val id: String? = null,
        val fins: Int? = null,
    ) : Pets_Pet

    private sealed interface Pets_Pet

    private data class People_Human(
        val id: String? = null,
        val name: String? = null,
    )

    private data class Foo_Foo(
        val id: String? = null,
        val dataId: String? = null,
    )
}
