package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars
import graphql.scalars.alias.AliasedScalar

class `date time scalar as input type` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo(input: "2022-03-09T05:01:50Z") {
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
                  foo(input: DateTime): Foo
                }
                type Foo {
                  thing: JSON
                }
                scalar JSON
                scalar DateTime
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  foo(input: DateTime): Foo
                }
                type Foo {
                  thing: JSON
                }
                scalar JSON
                scalar DateTime
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        if (env.getArgument<Any?>("input") == "2022-03-09T05:01:50Z") {
                            Service_Foo(thing = "What, were you expecting something else?")
                        } else {
                            null
                        }
                    }
                }
                wiring.scalar(
                    AliasedScalar
                        .Builder()
                        .name("DateTime")
                        .aliasedScalar(ExtendedScalars.Json)
                        .build(),
                )
                wiring.scalar(ExtendedScalars.Json)
            },
        ),
    ),
) {
    private data class Service_Foo(
        val thing: Any? = null,
    )
}
