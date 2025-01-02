package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `deep rename not asked for with unions` : NadelLegacyIntegrationTest(query = """
|query {
|  names {
|    ... on JiraIssue {
|      name
|    }
|    ... on Edible {
|      name
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Issues",
    overallSchema="""
    |type Query {
    |  names: [HasName]
    |}
    |union HasName = JiraIssue | Edible | Troll | User
    |type JiraIssue @renamed(from: "Issue") {
    |  name: String
    |}
    |type Edible {
    |  name: String
    |}
    |type Troll {
    |  name: String @renamed(from: "firstEat.item.name")
    |}
    |type User {
    |  name: String @renamed(from: "details.firstName")
    |}
    |""".trimMargin(), underlyingSchema="""
    |union HasName = Issue | Edible | Troll | User
    |type Query {
    |  names: [HasName]
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
    |type Issue {
    |  name: String
    |}
    |type UserDetails {
    |  firstName: String
    |}
    |type User {
    |  id: ID
    |  details: UserDetails
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("names") { env ->
          listOf(Issues_Issue(name = "GQLGW-001"), Issues_Issue(name = "GQLGW-1102"),
              Issues_Edible(name = "Spaghetti"))}
      }
      wiring.type("HasName") { type ->
        type.typeResolver { typeResolver ->
          val obj = typeResolver.getObject<Any>()
          val typeName = obj.javaClass.simpleName.substringAfter("_")
          typeResolver.schema.getTypeAs(typeName)
        }
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
  ) : Issues_HasName

  private sealed interface Issues_HasName

  private data class Issues_Issue(
    public val name: String? = null,
  ) : Issues_HasName

  private data class Issues_Troll(
    public val id: String? = null,
    public val firstEat: Issues_EatLog? = null,
  ) : Issues_HasName

  private data class Issues_User(
    public val id: String? = null,
    public val details: Issues_UserDetails? = null,
  ) : Issues_HasName

  private data class Issues_UserDetails(
    public val firstName: String? = null,
  )
}
