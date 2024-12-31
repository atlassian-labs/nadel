package graphql.nadel.tests.legacy.`document variable handling`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars

class `inlined json arguments` : NadelLegacyIntegrationTest(
    query = """
        query myQuery {
          hello(arg: {payload: {name: "Bobert", age: "23"}}, arg1: {interests: ["photography", "basketball"]})
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "MyService",
            overallSchema = """
                type Query {
                  hello(arg: InputWithJson, arg1: JSON!): String
                }
                input InputWithJson {
                  payload: JSON
                }
                scalar JSON
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  hello(arg: InputWithJson, arg1: JSON!): String
                }
                input InputWithJson {
                  payload: JSON
                }
                scalar JSON
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("hello") { env ->
                        if (env.getArgument<Any?>("arg") ==
                            mapOf(
                                "payload" to
                                    mapOf(
                                        "name" to "Bobert",
                                        "age" to
                                            "23",
                                    ),
                            ) &&
                            env.getArgument<Any?>("arg1") ==
                            mapOf(
                                "interests" to
                                    listOf(
                                        "photography",
                                        "basketball",
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
    private data class MyService_InputWithJson(
        val payload: Any? = null,
    )
}
