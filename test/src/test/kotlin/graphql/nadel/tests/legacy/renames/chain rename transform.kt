package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.collections.List

public class `chain rename transform` : NadelLegacyIntegrationTest(query = """
|query {
|  test(arg: "Hello World") {
|    __typename
|    id
|    cities(continent: Oceania)
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="MyService",
    overallSchema="""
    |type Query {
    |  test(arg: String): World @renamed(from: "world")
    |}
    |type World {
    |  id: ID
    |  cities(continent: Continent): [String] @renamed(from: "places")
    |}
    |enum Continent {
    |  Africa
    |  Antarctica
    |  Asia
    |  Oceania
    |  Europe
    |  NorthAmerica
    |  SouthAmerica
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  world(arg: String): World
    |}
    |type World {
    |  id: ID
    |  places(continent: Continent): [String]
    |}
    |enum Continent {
    |  Africa
    |  Antarctica
    |  Asia
    |  Oceania
    |  Europe
    |  NorthAmerica
    |  SouthAmerica
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("world") { env ->
          if (env.getArgument<Any?>("arg") == "aaarrg") {
            MyService_World(id = "Earth", places = listOf("Uhh yea I know cities"))}
          else {
            null}
        }
      }
    }
    )
)) {
  private enum class MyService_Continent {
    Africa,
    Antarctica,
    Asia,
    Oceania,
    Europe,
    NorthAmerica,
    SouthAmerica,
  }

  private data class MyService_World(
    public val id: String? = null,
    public val places: List<String?>? = null,
  )
}
