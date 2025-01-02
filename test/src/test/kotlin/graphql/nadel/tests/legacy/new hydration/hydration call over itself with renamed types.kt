package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `hydration call over itself with renamed types` : NadelLegacyIntegrationTest(
    query = """
        query {
          testing {
            movies {
              id
              name
              characters {
                id
                name
              }
              ...F1
            }
          }
        }
        fragment F1 on TestingMovie {
          name
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "testing",
            overallSchema = """
                type Query {
                  testing: Testing
                  characters(ids: [ID!]!): [TestingCharacter]
                }
                type Testing {
                  movies: [TestingMovie]
                }
                type TestingCharacter @renamed(from: "Character") {
                  id: ID!
                  name: String
                }
                type TestingMovie @renamed(from: "Movie") {
                  id: ID!
                  name: String
                  characters: [TestingCharacter]
                  @hydrated(
                    service: "testing"
                    field: "characters"
                    arguments: [{name: "ids" value: "${'$'}source.characterIds"}]
                    identifiedBy: "id"
                    batchSize: 3
                  )
                }
            """.trimIndent(),
            underlyingSchema = """
                type Character {
                  id: ID!
                  name: String
                }
                type Movie {
                  characterIds: [ID]
                  id: ID!
                  name: String
                }
                type Query {
                  characters(ids: [ID!]!): [Character]
                  testing: Testing
                }
                type Testing {
                  movies: [Movie]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    val charactersById = listOf(
                        Testing_Character(id = "C1", name = "Luke"),
                        Testing_Character(id = "C2", name = "Leia"),
                        Testing_Character(id = "C3", name = "Anakin"),
                    ).associateBy { it.id }

                    type
                        .dataFetcher("testing") { env ->
                            Testing_Testing(
                                movies = listOf(
                                    Testing_Movie(
                                        characterIds = listOf("C1", "C2"),
                                        id =
                                        "M1",
                                        name = "Movie 1",
                                    ),
                                    Testing_Movie(
                                        characterIds = listOf("C1", "C2", "C3"),
                                        id =
                                        "M2",
                                        name = "Movie 2",
                                    ),
                                ),
                            )
                        }
                        .dataFetcher("characters") { env ->
                            env.getArgument<List<String>>("ids")?.map(charactersById::get)
                        }
                }
            },
        ),
    ),
) {
    private data class Testing_Character(
        val id: String? = null,
        val name: String? = null,
    )

    private data class Testing_Movie(
        val characterIds: List<String?>? = null,
        val id: String? = null,
        val name: String? = null,
    )

    private data class Testing_Testing(
        val movies: List<Testing_Movie?>? = null,
    )
}
