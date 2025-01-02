package graphql.nadel.tests.legacy.introspection

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `if there are a mix of system fields and normal fields it works` :
    NadelLegacyIntegrationTest(query = """
|query {
|  __schema {
|    queryType {
|      name
|    }
|  }
|  __typename
|  hello {
|    name
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
    |""".trimMargin(), underlyingSchema="""
    |type Mutation {
    |  hello: String
    |}
    |
    |type Query {
    |  hello: World
    |}
    |
    |type World {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("hello") { env ->
          MyService_World(name = "World")}
      }
    }
    )
)) {
  private data class MyService_World(
    public val id: String? = null,
    public val name: String? = null,
  )
}
