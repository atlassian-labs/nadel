package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars
import graphql.scalars.alias.AliasedScalar

class `url scalar as input type` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo(input: "https://atlassian.com") {
            thing
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "service",
            overallSchema = """
                type Query {
                  foo(input: URL): Foo
                }
                type Foo {
                  thing: JSON
                }
                scalar JSON
                scalar URL
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  foo(input: URL): Foo
                }
                type Foo {
                  thing: JSON
                }
                scalar JSON
                scalar URL
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        if (env.getArgument<Any?>("input") == "https://atlassian.com") {
                            Service_Foo(thing = "What, were you expecting something else?")
                        } else {
                            null
                        }
                    }
                }
                wiring.scalar(ExtendedScalars.Json)
                wiring.scalar(
                    AliasedScalar
                        .Builder()
                        .name("URL")
                        .aliasedScalar(ExtendedScalars.Json)
                        .build(),
                )
            },
        ),
    ),
) {
    private data class Service_Foo(
        val thing: Any? = null,
    )
}
