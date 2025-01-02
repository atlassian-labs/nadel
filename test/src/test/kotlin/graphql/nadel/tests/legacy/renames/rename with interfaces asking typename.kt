package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `rename with interfaces asking typename` : NadelLegacyIntegrationTest(query = """
|query {
|  nodes {
|    __typename
|    id
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Issues",
    overallSchema="""
    |type Query {
    |  nodes: [Node]
    |}
    |type JiraIssue implements Node @renamed(from: "Issue") {
    |  id: ID!
    |}
    |interface Node {
    |  id: ID!
    |}
    |type User implements Node {
    |  id: ID! @renamed(from: "ari")
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  nodes: [Node]
    |}
    |interface Node {
    |  id: ID!
    |}
    |type Issue implements Node {
    |  id: ID!
    |}
    |type User implements Node {
    |  id: ID!
    |  ari: ID!
    |  name: String!
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("nodes") { env ->
          listOf(Issues_Issue(id = "GQLGW-001"), Issues_Issue(id = "GQLGW-1102"), Issues_User(ari =
              "ari:i-always-forget-the-format/1"))}
      }
      wiring.type("Node") { type ->
        type.typeResolver { typeResolver ->
          val obj = typeResolver.getObject<Any>()
          val typeName = obj.javaClass.simpleName.substringAfter("_")
          typeResolver.schema.getTypeAs(typeName)
        }
      }
    }
    )
)) {
  private data class Issues_Issue(
    override val id: String? = null,
  ) : Issues_Node

  private interface Issues_Node {
    public val id: String?
  }

  private data class Issues_User(
    override val id: String? = null,
    public val ari: String? = null,
    public val name: String? = null,
  ) : Issues_Node
}
