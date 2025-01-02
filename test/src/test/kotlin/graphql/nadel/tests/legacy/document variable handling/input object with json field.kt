package graphql.nadel.tests.legacy.`document variable handling`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars

class `input object with json field` : NadelLegacyIntegrationTest(
    query = """
        query myQuery(${'$'}var: JSON!) {
          hello(arg: {payload: ${'$'}var})
        }
    """.trimIndent(),
    variables = mapOf("var" to mapOf("48x48" to "file.jpeg")),
    services = listOf(
        Service(
            name = "MyService",
            overallSchema = """
                type Query {
                  hello(arg: InputWithJson): String
                }
                input InputWithJson {
                  payload: JSON
                }
                scalar JSON
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  hello(arg: InputWithJson): String
                }
                input InputWithJson {
                  payload: JSON
                }
                scalar JSON
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("hello") { env ->
                        if (env.getArgument<Any?>("arg") == mapOf("payload" to mapOf("48x48" to "file.jpeg"))) {
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
