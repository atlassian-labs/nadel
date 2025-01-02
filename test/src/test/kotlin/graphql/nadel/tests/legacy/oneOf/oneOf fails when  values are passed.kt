package graphql.nadel.tests.legacy.oneOf

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `oneOf fails when  values are passed` : NadelLegacyIntegrationTest(query = """
|query myQuery {
|  search(by: {name: "Figaro", id: "1001"})
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="MyService",
    overallSchema="""
    |type Query {
    |  search(by: SearchInput!): String
    |}
    |input SearchInput @oneOf {
    |  name: String
    |  id: ID
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  search(by: SearchInput!): String
    |}
    |input SearchInput @oneOf {
    |  name: String
    |  id: ID
    |}
    """.trimMargin(), runtimeWiring = { wiring ->
    }
    )
)) {
  private data class MyService_SearchInput(
    public val name: String? = null,
    public val id: String? = null,
  )
}
