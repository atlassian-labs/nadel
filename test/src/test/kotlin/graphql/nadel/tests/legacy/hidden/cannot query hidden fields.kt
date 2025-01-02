package graphql.nadel.tests.legacy.hidden

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `cannot query hidden fields` : NadelLegacyIntegrationTest(query = """
|query {
|  hello {
|    id
|    name
|    area51 {
|      name
|      coordinates
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="MyService",
    overallSchema="""
    |directive @hidden on FIELD_DEFINITION
    |type Query {
    |  hello: World
    |}
    |type World {
    |  id: ID
    |  name: String
    |  area51: Area @hidden
    |}
    |
    |type Area {
    |  name: String
    |  coordinates: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  hello: World
    |}
    |
    |type World {
    |  id: ID
    |  name: String
    |  area51: Area
    |}
    |
    |type Area {
    |  name: String
    |  coordinates: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
    }
    )
)) {
  private data class MyService_Area(
    public val name: String? = null,
    public val coordinates: String? = null,
  )

  private data class MyService_World(
    public val id: String? = null,
    public val name: String? = null,
    public val area51: MyService_Area? = null,
  )
}
