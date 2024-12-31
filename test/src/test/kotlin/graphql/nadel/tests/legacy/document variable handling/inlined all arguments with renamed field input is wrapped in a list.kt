package graphql.nadel.tests.legacy.`document variable handling`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars

class `inlined all arguments with renamed field input is wrapped in a list` : NadelLegacyIntegrationTest(
    query = """
        query myQuery(${'$'}varX: [InputWithJson])  {
          hello(arg: [{ age: 50, inputWithJson: ${'$'}varX }] ) {
            value
          }
        }
    """.trimIndent(),
    variables =
    mapOf(
        "varX" to
            listOf(
                mapOf(
                    "names" to listOf("Bobba", "Fett"),
                    "payload" to mapOf("name" to "Bobert", "age" to "23"),
                ),
            ),
    ),
    services = listOf(
        Service(
            name = "MyService",
            overallSchema = """
                type Query {
                  hello(arg: [InputArgType]): Output @renamed(from: "helloUnderlying")
                }
                input InputArgType @renamed(from: "UnderlyingInputArgType") {
                  age: Int
                  inputWithJson: [InputWithJson]
                }
                input InputWithJson @renamed(from: "InputWithJsonUnderlying") {
                  names: [String!]!
                  payload: JSON
                }
                type Output @renamed(from: "OutputUnderlying") {
                  value: String
                }
                scalar JSON
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  helloUnderlying(arg: [UnderlyingInputArgType]): OutputUnderlying
                }
                input UnderlyingInputArgType {
                  age: Int
                  inputWithJson: [InputWithJsonUnderlying]
                }
                input InputWithJsonUnderlying {
                  names: [String!]!
                  payload: JSON
                }
                type OutputUnderlying  {
                  value: String
                }
                scalar JSON
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("helloUnderlying") { env ->
                        if (env.getArgument<Any?>("arg") ==
                            listOf(
                                mapOf(
                                    "age" to 50,
                                    "inputWithJson" to
                                        listOf(
                                            mapOf(
                                                "names" to listOf("Bobba", "Fett"),
                                                "payload" to
                                                    mapOf(
                                                        "name" to
                                                            "Bobert",
                                                        "age" to "23",
                                                    ),
                                            ),
                                        ),
                                ),
                            )
                        ) {
                            MyService_OutputUnderlying(`value` = "world")
                        } else {
                            null
                        }
                    }
                }
                wiring.scalar(ExtendedScalars.Json)
            },
        ),
    ),
) {
    private data class MyService_InputWithJsonUnderlying(
        val names: List<String>? = null,
        val payload: Any? = null,
    )

    private data class MyService_OutputUnderlying(
        val `value`: String? = null,
    )

    private data class MyService_UnderlyingInputArgType(
        val age: Int? = null,
        val inputWithJson: List<MyService_InputWithJsonUnderlying?>? = null,
    )
}
