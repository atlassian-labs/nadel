package graphql.nadel.tests.legacy.`new hydration`.`polymorphic hydrations`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `batch polymorphic hydration actor fields are in the same service return types implement same interface` :
    NadelLegacyIntegrationTest(
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
        services =
        listOf(
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
                        service: "bar"
                        field: "petById"
                        arguments: [
                          {name: "ids" value: "${'$'}source.dataId"}
                        ]
                      )
                      @hydrated(
                        service: "bar"
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
            Service(
                name = "bar",
                overallSchema = """
                    type Query {
                      petById(ids: [ID]): [Pet]
                      humanById(ids: [ID]): [Human]
                    }
                    interface Node {
                      id: ID
                    }
                    type Human implements Node {
                      id: ID
                      name: String
                    }
                    type Pet implements Node {
                      id: ID
                      breed: String
                    }
                """.trimIndent(),
                underlyingSchema = """
                    type Query {
                      petById(ids: [ID]): [Pet]
                      humanById(ids: [ID]): [Human]
                    }
                    interface Node {
                      id: ID
                    }
                    type Human implements Node {
                      id: ID
                      name: String
                    }
                    type Pet implements Node {
                      id: ID
                      breed: String
                    }
                """.trimIndent(),
                runtimeWiring = { wiring ->
                    wiring.type("Query") { type ->
                        type
                            .dataFetcher("petById") { env ->
                                if (env.getArgument<Any?>("ids") == listOf("PET-0", "PET-1")) {
                                    listOf(
                                        Bar_Pet(breed = "Akita", id = "PET-0"),
                                        Bar_Pet(
                                            breed = "Labrador",
                                            id =
                                            "PET-1",
                                        ),
                                    )
                                } else {
                                    null
                                }
                            }
                            .dataFetcher("humanById") { env ->
                                if (env.getArgument<Any?>("ids") == listOf("HUMAN-0", "HUMAN-1")) {
                                    listOf(
                                        Bar_Human(id = "HUMAN-0", name = "Fanny Longbottom"),
                                        Bar_Human(
                                            id = "HUMAN-1",
                                            name = "John Doe",
                                        ),
                                    )
                                } else {
                                    null
                                }
                            }
                    }
                    wiring.type("Node") { type ->
                        type.typeResolver { typeResolver ->
                            val obj = typeResolver.getObject<Any>()
                            val typeName = obj.javaClass.simpleName.substringAfter("_")
                            typeResolver.schema.getTypeAs(typeName)
                        }
                    }
                },
            ),
        ),
    ) {
    private data class Foo_Foo(
        val id: String? = null,
        val dataId: String? = null,
    )

    private data class Bar_Human(
        override val id: String? = null,
        val name: String? = null,
    ) : Bar_Node

    private interface Bar_Node {
        val id: String?
    }

    private data class Bar_Pet(
        override val id: String? = null,
        val breed: String? = null,
    ) : Bar_Node
}
