package graphql.nadel.tests.legacy.`document variable handling`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars
import kotlin.Any

public class `primitive json arguments variables` : NadelLegacyIntegrationTest(query = """
|query myQuery(${'$'}var1: JSON!, ${'$'}var2: JSON!) {
|  hello(arg: {payload: ${'$'}var1}, arg1: ${'$'}var2)
|}
|""".trimMargin(), variables = mapOf("var1" to "String JSON input", "var2" to false), services =
    listOf(Service(name="MyService", overallSchema="""
    |type Query {
    |  hello(arg: InputWithJson, arg1: JSON!): String
    |}
    |
    |input InputWithJson {
    |  payload: JSON
    |}
    |
    |scalar JSON
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  hello(arg: InputWithJson, arg1: JSON!): String
    |}
    |
    |input InputWithJson {
    |  payload: JSON
    |}
    |
    |scalar JSON
    """.trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("hello") { env ->
          if (env.getArgument<Any?>("arg") == mapOf("payload" to "String JSON input") &&
              env.getArgument<Any?>("arg1") == false) {
            "world"}
          else {
            null}
        }
      }
      wiring.scalar(ExtendedScalars.Json)}
    )
)) {
  private data class MyService_InputWithJson(
    public val payload: Any? = null,
  )
}
