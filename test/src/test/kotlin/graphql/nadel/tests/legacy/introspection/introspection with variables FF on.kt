package graphql.nadel.tests.legacy.introspection

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `introspection with variables FF on` : NadelLegacyIntegrationTest(query = """
|query {
|  __schema {
|    queryType {
|      fields(includeDeprecated: true) {
|        name
|        isDeprecated
|      }
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="MyService",
    overallSchema="""
    |type Query {
    |  earth: Planet
    |  pluto: Planet @deprecated(reason: "Oh no")
    |}
    |type Planet {
    |  id: ID
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
    |  earth: Planet
    |  pluto: Planet @deprecated(reason: "Oh no")
    |}
    |
    |type Planet {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
    }
    )
)) {
  private data class MyService_Planet(
    public val id: String? = null,
    public val name: String? = null,
  )
}
