package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `deep rename returns null` : NadelLegacyIntegrationTest(query = """
|query {
|  troll {
|    name
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Issues",
    overallSchema="""
    |type Query {
    |  troll: Troll
    |}
    |type Troll {
    |  name: String @renamed(from: "firstEat.item.name")
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  troll: Troll
    |}
    |type Troll {
    |  id: ID
    |  firstEat: EatLog
    |}
    |type EatLog {
    |  id: ID
    |  item: Edible
    |}
    |type Edible {
    |  name: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("troll") { env ->
          Issues_Troll(firstEat = Issues_EatLog(item = null))}
      }
    }
    )
)) {
  private data class Issues_EatLog(
    public val id: String? = null,
    public val item: Issues_Edible? = null,
  )

  private data class Issues_Edible(
    public val name: String? = null,
  )

  private data class Issues_Troll(
    public val id: String? = null,
    public val firstEat: Issues_EatLog? = null,
  )
}
