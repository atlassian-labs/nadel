package graphql.nadel.tests.legacy.`document variable handling`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars
import kotlin.Any
import kotlin.Int
import kotlin.String
import kotlin.collections.List

public class `inlined all arguments with mixed literals and variables` :
    NadelLegacyIntegrationTest(query = """
|query myQuery(${'$'}varX: InputWithJson)  {
|  hello(arg: { age: 50, inputWithJson: ${'$'}varX } )
|}
|""".trimMargin(), variables = mapOf("varX" to mapOf("names" to listOf("Bobba", "Fett"), "payload"
    to mapOf("name" to "Bobert", "age" to "23"))), services = listOf(Service(name="MyService",
    overallSchema="""
    |type Query {
    |  hello(arg: InputArgType): String
    |}
    |
    |input InputArgType @renamed(from: "UnderlyingInputArgType") {
    |  age: Int
    |  inputWithJson: InputWithJson
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
    |  hello(arg: UnderlyingInputArgType): String
    |}
    |
    |input UnderlyingInputArgType {
    |  age: Int
    |  inputWithJson: InputWithJsonUnderlying
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
          if (env.getArgument<Any?>("arg") == mapOf("age" to 50, "inputWithJson" to mapOf("names" to
              listOf("Bobba", "Fett"), "payload" to mapOf("name" to "Bobert", "age" to "23")))) {
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

  private data class MyService_UnderlyingInputArgType(
    public val age: Int? = null,
    public val inputWithJson: MyService_InputWithJsonUnderlying? = null,
  )
}
