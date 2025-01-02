package graphql.nadel.tests.legacy.oneOf

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `oneOf fails when nested input` : NadelLegacyIntegrationTest(query = """
|query myQuery {
|  search(by: {id: {email: null}})
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="MyService",
    overallSchema="""
    |type Query {
    |  search(by: SearchInput): String
    |}
    |
    |input SearchInput {
    |  name: String
    |  id: IdInput
    |}
    |
    |input IdInput @oneOf {
    |  email: String
    |  id: ID
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  search(by: SearchInput): String
    |}
    |
    |input SearchInput {
    |  name: String
    |  id: IdInput
    |}
    |
    |input IdInput @oneOf {
    |  email: String
    |  id: ID
    |}
    """.trimMargin(), runtimeWiring = { wiring ->
    }
    )
)) {
  private data class MyService_IdInput(
    public val email: String? = null,
    public val id: String? = null,
  )

  private data class MyService_SearchInput(
    public val name: String? = null,
    public val id: MyService_IdInput? = null,
  )
}
