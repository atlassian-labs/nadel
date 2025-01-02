package graphql.nadel.tests.legacy.oneOf

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `oneOf fails when invalid variables are passed` : NadelLegacyIntegrationTest(query =
    """
|query myQuery(${'$'}name: String, ${'$'}id: ID) {
|  search(by: {name: ${'$'}name, id: ${'$'}id})
|}
|""".trimMargin(), variables = mapOf("name" to "Figaro"), services =
    listOf(Service(name="MyService", overallSchema="""
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
