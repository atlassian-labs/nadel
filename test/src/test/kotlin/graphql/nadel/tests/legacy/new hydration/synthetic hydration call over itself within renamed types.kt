package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

public class `synthetic hydration call over itself within renamed types` :
    NadelLegacyIntegrationTest(query = """
|query {
|  tests {
|    testing {
|      movies {
|        id
|        name
|        characters {
|          id
|          name
|        }
|      }
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="testing",
    overallSchema="""
    |type Query {
    |  tests: TestQuery
    |}
    |type TestQuery {
    |  testing: Testing
    |  characters(ids: [ID!]!): [TestingCharacter]
    |}
    |type Testing {
    |  movies: [TestingMovie]
    |}
    |type TestingCharacter @renamed(from: "Character") {
    |  id: ID!
    |  name: String
    |}
    |type TestingMovie @renamed(from: "Movie") {
    |  id: ID!
    |  name: String
    |  characters: [TestingCharacter]
    |  @hydrated(
    |    service: "testing"
    |    field: "tests.characters"
    |    arguments: [{name: "ids" value: "${'$'}source.characterIds"}]
    |    identifiedBy: "id"
    |    batchSize: 3
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Character {
    |  id: ID!
    |  name: String
    |}
    |
    |type Movie {
    |  characterIds: [ID]
    |  id: ID!
    |  name: String
    |}
    |
    |type Query {
    |  tests: TestQuery
    |}
    |
    |type TestQuery {
    |  characters(ids: [ID!]!): [Character]
    |  testing: Testing
    |}
    |
    |type Testing {
    |  movies: [Movie]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("tests") {
          Unit}
      }
      wiring.type("Query") { type ->
        type.dataFetcher("tests") { env ->
          Testing_TestQuery(testing = Testing_Testing(movies = listOf(Testing_Movie(characterIds =
              listOf("C1", "C2"), id = "M1", name = "Movie 1"), Testing_Movie(characterIds =
              listOf("C1", "C2", "C3"), id = "M2", name = "Movie 2"))))}
      }

      wiring.type("TestQuery") { type ->
        type.dataFetcher("characters") { env ->
          if (env.getArgument<Any?>("ids") == listOf("C1", "C2", "C3")) {
            listOf(Testing_Character(id = "C1", name = "Luke"), Testing_Character(id = "C2", name =
                "Leia"), Testing_Character(id = "C3", name = "Anakin"))}
          else {
            null}
        }
      }
    }
    )
)) {
  private data class Testing_Character(
    public val id: String? = null,
    public val name: String? = null,
  )

  private data class Testing_Movie(
    public val characterIds: List<String?>? = null,
    public val id: String? = null,
    public val name: String? = null,
  )

  private data class Testing_TestQuery(
    public val characters: List<Testing_Character?>? = null,
    public val testing: Testing_Testing? = null,
  )

  private data class Testing_Testing(
    public val movies: List<Testing_Movie?>? = null,
  )
}
