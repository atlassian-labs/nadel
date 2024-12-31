package graphql.nadel.tests.legacy.`document variable handling`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars

class `primitive json arguments` : NadelLegacyIntegrationTest(
    query = """
        query myQuery {
          hello(arg: {payload: "String JSON input"}, arg1: false)
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
                        if (env.getArgument<Any?>("arg") == mapOf("payload" to "String JSON input") &&
                            env.getArgument<Any?>("arg1") == false
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
