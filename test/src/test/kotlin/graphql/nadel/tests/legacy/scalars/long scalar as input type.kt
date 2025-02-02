package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars
import graphql.scalars.alias.AliasedScalar
import java.math.BigInteger

class `long scalar as input type` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo(input: 3000000000) {
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
                  foo(input: Long): Foo
                }
                type Foo {
                  thing: JSON
                }
                scalar JSON
                scalar Long
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  foo(input: Long): Foo
                }
                type Foo {
                  thing: JSON
                }
                scalar JSON
                scalar Long
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("foo") { env ->
                        if (env.getArgument<BigInteger?>("input")?.toLong() == 3_000_000_000L) {
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
                        .name("Long")
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
