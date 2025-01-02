package graphql.nadel.tests.legacy.hidden

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `can query non hidden fields` : NadelLegacyIntegrationTest(query = """
|query {
|  hello {
|    id
|    name
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="MyService",
    overallSchema="""
    |directive @hidden on FIELD_DEFINITION
    |type Query {
    |  hello: World
    |  hiddenField: String @hidden
    |}
    |type World {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  hello: World
    |  hiddenField: String
    |}
    |
    |type World {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("hello") { env ->
          MyService_World(id = "ID", name = "World")}
      }
    }
    )
)) {
  private data class MyService_World(
    public val id: String? = null,
    public val name: String? = null,
  )
}
