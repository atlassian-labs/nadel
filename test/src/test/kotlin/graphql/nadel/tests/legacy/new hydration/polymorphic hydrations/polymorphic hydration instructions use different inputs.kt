package graphql.nadel.tests.legacy.`new hydration`.`polymorphic hydrations`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `polymorphic hydration instructions use different inputs` : NadelLegacyIntegrationTest(
    query = """
        query {
          petsByIds(ids: ["good-boye-1", "tall-boye-9"]) {
            animal {
              __typename
              ... on Dog {
                name
              }
              ... on Cat {
                name
              }
              ... on Giraffe {
                name
                birthday
                height
              }
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "Pets",
            overallSchema = """
                type Query {
                  petsByIds(ids: [ID!]!): [Pet]
                }
                union Animal = Dog | Cat | Giraffe
                type Pet {
                  animal: Animal
                  @hydrated(
                    service: "Dogs"
                    field: "dogsByIds"
                    arguments: [{ name: "ids", value: "${'$'}source.animalId"}]
                    identifiedBy: "id"
                    batchSize: 90
                  )
                  @hydrated(
                    service: "Cats"
                    field: "cats.catsByIds"
                    arguments: [{ name: "ids", value: "${'$'}source.animalId"}]
                    identifiedBy: "id"
                    batchSize: 90
                  )
                  @hydrated(
                    service: "Zoo"
                    field: "giraffes"
                    arguments: [{ name: "filters", value: "${'$'}source.giraffeInput"}]
                    inputIdentifiedBy: [
                      {sourceId: "giraffeInput.nickname", resultId: "nickname"}
                      {sourceId: "giraffeInput.birthday", resultId: "birthday"}
                      {sourceId: "giraffeInput.height", resultId: "height"}
                    ]
                    batchSize: 90
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  petsByIds(ids: [ID!]!): [Pet]
                }
                type Pet {
                  animalId: ID!
                  giraffeInput: PetGiraffeInput
                }
                type PetGiraffeInput {
                  nickname: String!
                  birthday: Int!
                  height: Int!
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    val petById = listOf(
                        Pets_Pet(
                            animalId = "good-boye-1",
                            giraffeInput = null
                        ),
                        Pets_Pet(
                            animalId = "tall-boye-9",
                            giraffeInput = Pets_PetGiraffeInput(
                                nickname = "Tall Boye",
                                birthday = 1_001_203_200,
                                height = 570,
                            ),
                        ),
                    ).associateBy { it.animalId }

                    type
                        .dataFetcher("petsByIds") { env ->
                            env.getArgument<List<String>>("ids")?.map(petById::get)
                        }
                }
            },
        ),
        Service(
            name = "Dogs",
            overallSchema = """
                type Query {
                  dogsByIds(ids: [ID!]!): [Dog]
                }
                type Dog {
                  id: ID!
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  dogsByIds(ids: [ID!]!): [Dog]
                }
                type Dog {
                  id: ID!
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("dogsByIds") { env ->
                        if (env.getArgument<Any?>("ids") == listOf("good-boye-1")) {
                            listOf(Dogs_Dog(id = "good-boye-1", name = "Abe"))
                        } else {
                            null
                        }
                    }
                }
            },
        ),
        Service(
            name = "Cats",
            overallSchema = """
                type Query {
                  cats: CatQuery
                }
                type CatQuery {
                  catsByIds(ids: [ID!]!): [Cat]
                }
                type Cat {
                  id: ID!
                  name: String
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  cats: CatQuery
                }
                type CatQuery {
                  catsByIds(ids: [ID!]!): [Cat]
                }
                type Cat {
                  id: ID!
                  name: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
        Service(
            name = "Zoo",
            overallSchema = """
                type Query {
                  giraffes(filters: [GiraffeFilter]): [Giraffe]
                }
                input GiraffeFilter {
                  nickname: String!
                  birthday: Int!
                  height: Int!
                }
                type Giraffe {
                  id: ID!
                  name: String
                  nickname: String
                  birthday: Int
                  height: Int
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  giraffes(filters: [GiraffeFilter]): [Giraffe]
                }
                input GiraffeFilter {
                  nickname: String!
                  birthday: Int!
                  height: Int!
                }
                type Giraffe {
                  id: ID!
                  name: String
                  nickname: String
                  birthday: Int
                  height: Int
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("giraffes") { env ->
                        if (env.getArgument<Any?>("filters") ==
                            listOf(
                                mapOf(
                                    "nickname" to "Tall Boye",
                                    "birthday"
                                        to 1_001_203_200,
                                    "height" to 570,
                                ),
                            )
                        ) {
                            listOf(
                                Zoo_Giraffe(
                                    birthday = 1_001_203_200,
                                    height = 570,
                                    name = "Rukiya",
                                    nickname =
                                    "Tall Boye",
                                ),
                            )
                        } else {
                            null
                        }
                    }
                }
            },
        ),
    ),
) {
    private data class Pets_Pet(
        val animalId: String? = null,
        val giraffeInput: Pets_PetGiraffeInput? = null,
    )

    private data class Pets_PetGiraffeInput(
        val nickname: String? = null,
        val birthday: Int? = null,
        val height: Int? = null,
    )

    private data class Dogs_Dog(
        val id: String? = null,
        val name: String? = null,
    )

    private data class Cats_Cat(
        val id: String? = null,
        val name: String? = null,
    )

    private data class Cats_CatQuery(
        val catsByIds: List<Cats_Cat?>? = null,
    )

    private data class Zoo_Giraffe(
        val id: String? = null,
        val name: String? = null,
        val nickname: String? = null,
        val birthday: Int? = null,
        val height: Int? = null,
    )

    private data class Zoo_GiraffeFilter(
        val nickname: String? = null,
        val birthday: Int? = null,
        val height: Int? = null,
    )
}
