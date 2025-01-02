package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `hydration with more interfaces` : NadelLegacyIntegrationTest(query = """
|query {
|  nodes {
|    id
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Issues",
    overallSchema="""
    |type Query {
    |  nodes: [Node]
    |  trollName(id: ID!): String
    |  ariById(id: ID!): ID
    |}
    |type JiraIssue implements Node @renamed(from: "Issue") {
    |  id: ID
    |}
    |type Troll implements Node {
    |  id: ID
    |  @hydrated(
    |    service: "Issues"
    |    field: "trollName"
    |    arguments: [{name: "id" value: "${'$'}source.id"}]
    |  )
    |}
    |interface Node {
    |  id: ID
    |}
    |type User implements Node {
    |  id: ID
    |  @hydrated(
    |    service: "Issues"
    |    field: "ariById"
    |    arguments: [{name: "id" value: "${'$'}source.id"}]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  trollName(id: ID!): String
    |  ariById(id: ID!): ID
    |  nodes: [Node]
    |}
    |interface Node {
    |  id: ID
    |}
    |type Troll implements Node {
    |  id: ID
    |  nameOfFirstThingEaten: String
    |}
    |type Issue implements Node {
    |  id: ID
    |}
    |type User implements Node {
    |  id: ID
    |  ari: ID
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("nodes") { env ->
          listOf(Issues_Issue(id = "GQLGW-001"), Issues_Issue(id = "GQLGW-1102"), Issues_Troll(id =
              "My Arm"), Issues_User(id = "Franklin"), Issues_Issue(id = "GQLGW-11"))}

        .dataFetcher("trollName") { env ->
          if (env.getArgument<Any?>("id") == "My Arm") {
            "Troll"}
          else {
            null}
        }

        .dataFetcher("ariById") { env ->
          if (env.getArgument<Any?>("id") == "Franklin") {
            "ari:user/Franklin"}
          else {
            null}
        }
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

  private data class Issues_Troll(
    override val id: String? = null,
    public val nameOfFirstThingEaten: String? = null,
  ) : Issues_Node

  private data class Issues_User(
    override val id: String? = null,
    public val ari: String? = null,
  ) : Issues_Node
}
