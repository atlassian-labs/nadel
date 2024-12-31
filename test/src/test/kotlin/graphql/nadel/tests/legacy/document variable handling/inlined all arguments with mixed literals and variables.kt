package graphql.nadel.tests.legacy.`document variable handling`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars

class `inlined all arguments with mixed literals and variables` : NadelLegacyIntegrationTest(
    query = """
        query myQuery(${'$'}varX: InputWithJson)  {
          hello(arg: { age: 50, inputWithJson: ${'$'}varX } )
        }
    """.trimIndent(),
    variables =
    mapOf(
        "varX" to
            mapOf(
                "names" to listOf("Bobba", "Fett"),
                "payload"
                    to mapOf("name" to "Bobert", "age" to "23"),
            ),
    ),
    services = listOf(
        Service(
            name = "MyService",
            overallSchema = """
                type Query {
                  hello(arg: InputArgType): String
                }
                input InputArgType @renamed(from: "UnderlyingInputArgType") {
                  age: Int
                  inputWithJson: InputWithJson
                }
                input InputWithJson @renamed(from: "InputWithJsonUnderlying") {
                  names: [String!]!
                  payload: JSON
                }
                scalar JSON
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  hello(arg: UnderlyingInputArgType): String
                }
                input UnderlyingInputArgType {
                  age: Int
                  inputWithJson: InputWithJsonUnderlying
                }
                input InputWithJsonUnderlying {
                  names: [String!]!
                  payload: JSON
                }
                scalar JSON
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("hello") { env ->
                        if (env.getArgument<Any?>("arg") ==
                            mapOf(
                                "age" to 50,
                                "inputWithJson" to
                                    mapOf(
                                        "names" to
                                            listOf("Bobba", "Fett"),
                                        "payload" to mapOf("name" to "Bobert", "age" to "23"),
                                    ),
                            )
                        ) {
                            "world"
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

    private data class MyService_UnderlyingInputArgType(
        val age: Int? = null,
        val inputWithJson: MyService_InputWithJsonUnderlying? = null,
    )
}
