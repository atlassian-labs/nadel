package graphql.nadel.tests.legacy.`document variable handling`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars
import kotlin.Any
import kotlin.String
import kotlin.collections.List

public class `inlined all arguments` : NadelLegacyIntegrationTest(query = """
|query myQuery(${'$'}inputVar1: String, ${'$'}inputVar2: String! = "defaulted", ${'$'}inputVar3: String) {
|  hello(
|        arg: { names: ["Bobba", "Fett"] payload: {name: "Bobert", age: "23"}}, 
|        arg1: {interests: ["photography", "basketball"]},
|        arg2: null,
|        arg3: ${'$'}inputVar1,
|        arg4Nullable: ${'$'}inputVar2
|        arg5Nullable: ${'$'}inputVar3
|  )
|}
|""".trimMargin(), variables = mapOf("inputVar1" to "input1"), services =
    listOf(Service(name="MyService", overallSchema="""
    |type Query {
    |  hello(
    |    arg: InputWithJson,
    |    arg1: JSON!,
    |    arg2: String,
    |    arg3: String,
    |    arg4Nullable: String,
    |    arg5Nullable: String
    |  ): String
    |}
    |
    |input InputWithJson @renamed(from: "InputWithJsonUnderlying") {
    |  names: [String!]!
    |  payload: JSON
    |}
    |
    |scalar JSON
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  hello(arg: InputWithJsonUnderlying,
    |    arg1: JSON!, arg2: String, arg3: String, arg4Nullable: String, arg5Nullable: String): String
    |}
    |
    |input InputWithJsonUnderlying {
    |  names: [String!]!
    |  payload: JSON
    |}
    |
    |scalar JSON
    """.trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("hello") { env ->
          if (env.getArgument<Any?>("arg") == mapOf("names" to listOf("Bobba", "Fett"), "payload" to
              mapOf("name" to "Bobert", "age" to "23")) && env.getArgument<Any?>("arg1") ==
              mapOf("interests" to listOf("photography", "basketball")) &&
              env.getArgument<Any?>("arg2") == null && env.getArgument<Any?>("arg3") == "input1" &&
              env.getArgument<Any?>("arg4Nullable") == "defaulted") {
            "world"}
          else {
            null}
        }
      }
      wiring.scalar(ExtendedScalars.Json)}
    )
)) {
  private data class MyService_InputWithJsonUnderlying(
    public val names: List<String>? = null,
    public val payload: Any? = null,
  )
}
