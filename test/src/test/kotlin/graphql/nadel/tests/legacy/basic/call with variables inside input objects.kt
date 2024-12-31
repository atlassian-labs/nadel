package graphql.nadel.tests.legacy.basic

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `call with variables inside input objects` : NadelLegacyIntegrationTest(
    query = """
        query myQuery(${'$'}varIds: [ID], ${'$'}otherVar: String) {
          hello(arg: {ids: ${'$'}varIds}, otherArg: ${'$'}otherVar)
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "MyService",
            overallSchema = """
                type Query {
                  hello(arg: Arg, otherArg: String): String
                }
                input Arg {
                  ids: [ID]
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  hello(arg: Arg, otherArg: String): String
                }
                input Arg {
                  ids: [ID]
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("hello") { env ->
                        if (env.getArgument<Any?>("arg") == emptyMap<Any?, Any?>()) {
                            "world"
                        } else {
                            null
                        }
                    }
                }
            },
        ),
    ),
) {
    private data class MyService_Arg(
        val ids: List<String?>? = null,
    )
}
