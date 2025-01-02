package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `deep rename with interfaces` : NadelLegacyIntegrationTest(query = """
|query {
|  names {
|    name
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Issues",
    overallSchema="""
    |type Query {
    |  names: [HasName]
    |}
    |type JiraIssue implements HasName @renamed(from: "Issue") {
    |  name: String
    |}
    |interface HasName {
    |  name: String
    |}
    |type User implements HasName {
    |  name: String @renamed(from: "details.firstName")
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  names: [HasName]
    |}
    |interface HasName {
    |  name: String
    |}
    |type Issue implements HasName {
    |  name: String
    |}
    |type UserDetails implements HasName {
    |  name: String
    |  firstName: String
    |}
    |type User implements HasName {
    |  id: ID
    |  name: String
    |  details: UserDetails
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("names") { env ->
          listOf(Issues_Issue(name = "GQLGW-001"), Issues_Issue(name = "GQLGW-1102"),
              Issues_User(details = Issues_UserDetails(firstName = "Franklin")))}
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
  private interface Issues_HasName {
    public val name: String?
  }

  private data class Issues_Issue(
    override val name: String? = null,
  ) : Issues_HasName

  private data class Issues_User(
    public val id: String? = null,
    override val name: String? = null,
    public val details: Issues_UserDetails? = null,
  ) : Issues_HasName

  private data class Issues_UserDetails(
    override val name: String? = null,
    public val firstName: String? = null,
  ) : Issues_HasName
}
