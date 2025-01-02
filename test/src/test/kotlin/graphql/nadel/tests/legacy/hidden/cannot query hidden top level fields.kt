package graphql.nadel.tests.legacy.hidden

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `cannot query hidden top level fields` : NadelLegacyIntegrationTest(query = """
|query {
|  hiddenField
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
    }
    )
)) {
  private data class MyService_World(
    public val id: String? = null,
    public val name: String? = null,
  )
}
