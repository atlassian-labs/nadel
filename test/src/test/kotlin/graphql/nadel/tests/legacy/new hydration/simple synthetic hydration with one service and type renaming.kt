package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.Unit

public class `simple synthetic hydration with one service and type renaming` :
    NadelLegacyIntegrationTest(query = """
|query {
|  tests {
|    testing {
|      movie {
|        id
|        name
|        character {
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
    |  character(id: ID): TestingCharacter
    |}
    |type Testing {
    |  movie: Movie
    |}
    |type TestingCharacter @renamed(from: "Character") {
    |  id: ID!
    |  name: String
    |}
    |type Movie {
    |  id: ID!
    |  name: String
    |  character: TestingCharacter
    |  @hydrated(
    |    service: "testing"
    |    field: "tests.character"
    |    arguments: [{name: "id" value: "${'$'}source.characterId"}]
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
    |  characterId: ID
    |  id: ID!
    |  name: String
    |}
    |
    |type Query {
    |  tests: TestQuery
    |}
    |
    |type TestQuery {
    |  character(id: ID): Character
    |  testing: Testing
    |}
    |
    |type Testing {
    |  movie: Movie
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("tests") {
          Unit}
      }
      wiring.type("Query") { type ->
        type.dataFetcher("tests") { env ->
          Testing_TestQuery(testing = Testing_Testing(movie = Testing_Movie(characterId = "C1", id =
              "M1", name = "Movie 1")))}
      }

      wiring.type("TestQuery") { type ->
        type.dataFetcher("character") { env ->
          if (env.getArgument<Any?>("id") == "C1") {
            Testing_Character(id = "C1", name = "Luke")}
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
    public val characterId: String? = null,
    public val id: String? = null,
    public val name: String? = null,
  )

  private data class Testing_TestQuery(
    public val character: Testing_Character? = null,
    public val testing: Testing_Testing? = null,
  )

  private data class Testing_Testing(
    public val movie: Testing_Movie? = null,
  )
}
