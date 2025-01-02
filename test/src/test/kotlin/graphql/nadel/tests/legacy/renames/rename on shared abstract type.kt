package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `rename on shared abstract type` : NadelLegacyIntegrationTest(query = """
|query {
|  node(id: "world-1") {
|    id
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="shared",
    overallSchema="""
    |interface Node {
    |  id: ID!
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  echo: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
    }
    )
, Service(name="worlds", overallSchema="""
    |type Query {
    |  node(id: ID): Node
    |}
    |type World implements Node {
    |  id: ID!
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  node(id: ID): Node
    |  worlds: [World]
    |}
    |type World implements Node {
    |  id: ID!
    |}
    |interface Node {
    |  id: ID!
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("node") { env ->
          if (env.getArgument<Any?>("id") == "world-1") {
            Worlds_World(id = "Test")}
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
, Service(name="planets", overallSchema="""
    |type Planet implements Node {
    |  id: ID! @renamed(from: "identifier")
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  echo: String
    |}
    |type Planet implements Node {
    |  identifier: ID!
    |  id: ID!
    |}
    |interface Node {
    |  id: ID!
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
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
  private interface Planets_Node {
    public val id: String?
  }

  private data class Planets_Planet(
    public val identifier: String? = null,
    override val id: String? = null,
  ) : Planets_Node

  private interface Worlds_Node {
    public val id: String?
  }

  private data class Worlds_World(
    override val id: String? = null,
  ) : Worlds_Node
}
