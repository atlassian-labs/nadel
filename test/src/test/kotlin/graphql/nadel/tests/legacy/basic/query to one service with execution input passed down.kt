package graphql.nadel.tests.legacy.basic

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `query to one service with execution input passed down` :
    NadelLegacyIntegrationTest(query = """
|query OpName {
|  hello {
|    name
|  }
|  hello {
|    id
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="MyService",
    overallSchema="""
    |type Query {
    |  hello: World
    |}
    |type World {
    |  id: ID
    |  name: String
    |}
    |type Mutation {
    |  hello: String
    |}
    |type Subscription {
    |  onWorldUpdate: World
    |  onAnotherUpdate: World
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Mutation {
    |  hello: String
    |}
    |
    |type Query {
    |  hello: World
    |}
    |
    |type Subscription {
    |  onAnotherUpdate: World
    |  onWorldUpdate: World
    |}
    |
    |type World {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("hello") { env ->
          MyService_World(id = "3", name = "earth")}
      }
    }
    )
)) {
  private data class MyService_World(
    public val id: String? = null,
    public val name: String? = null,
  )
}
