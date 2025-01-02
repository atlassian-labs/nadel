package graphql.nadel.tests.legacy.hidden

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `introspection does not show hidden fields` : NadelLegacyIntegrationTest(query = """
|query introspection_query {
|  __schema {
|    queryType {
|      fields(includeDeprecated: false) {
|        name
|      }
|    }
|  }
|
|  __type(name: "World") {
|    name
|    fields {
|      name
|    }
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
    |  area51: String @hidden
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
    |  area51: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
    }
    )
)) {
  private data class MyService_World(
    public val id: String? = null,
    public val name: String? = null,
    public val area51: String? = null,
  )
}
