package graphql.nadel.tests.legacy.oneOf

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `oneOf successful` : NadelLegacyIntegrationTest(query = """
|query myQuery {
|  search(by: {name: "Figaro"})
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="MyService",
    overallSchema="""
    |type Query {
    |  search(by: SearchInput): String
    |}
    |input SearchInput @oneOf {
    |  name: String
    |  id: ID
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  search(by: SearchInput): String
    |}
    |input SearchInput @oneOf {
    |  name: String
    |  id: ID
    |}
    """.trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("search") { env ->
          if (env.getArgument<Any?>("by") == mapOf("name" to "Figaro")) {
            "Figaro"}
          else {
            null}
        }
      }
    }
    )
)) {
  private data class MyService_SearchInput(
    public val name: String? = null,
    public val id: String? = null,
  )
}
