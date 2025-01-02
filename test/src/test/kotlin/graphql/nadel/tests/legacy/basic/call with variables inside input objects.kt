package graphql.nadel.tests.legacy.basic

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.collections.List

public class `call with variables inside input objects` : NadelLegacyIntegrationTest(query = """
|query myQuery(${'$'}varIds: [ID], ${'$'}otherVar: String) {
|  hello(arg: {ids: ${'$'}varIds}, otherArg: ${'$'}otherVar)
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="MyService",
    overallSchema="""
    |type Query {
    |  hello(arg: Arg, otherArg: String): String
    |}
    |input Arg {
    |  ids: [ID]
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  hello(arg: Arg, otherArg: String): String
    |}
    |
    |input Arg {
    |  ids: [ID]
    |}
    """.trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("hello") { env ->
          if (env.getArgument<Any?>("arg") == emptyMap<Any?, Any?>()) {
            "world"}
          else {
            null}
        }
      }
    }
    )
)) {
  private data class MyService_Arg(
    public val ids: List<String?>? = null,
  )
}
