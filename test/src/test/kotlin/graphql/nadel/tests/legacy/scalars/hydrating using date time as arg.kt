package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars
import graphql.scalars.alias.AliasedScalar

class `hydrating using date time as arg` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo {
            successor {
              id
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "service",
            overallSchema = """
                type Query {
                  foo: Foo
                  successor(after: DateTime): Foo
                }
                type Foo {
                  id: ID
                  createdAt: DateTime
                  successor: Foo
                  @hydrated(
                    service: "service"
                    field: "successor"
                    arguments: [{ name: "after" value: "${'$'}source.createdAt" }]
                  )
                }
                scalar DateTime
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  foo: Foo
                  successor(after: DateTime): Foo
                }
                type Foo {
                  id: ID
                  createdAt: DateTime
                  successor: Foo
                }
                scalar DateTime
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type
                        .dataFetcher("foo") { env ->
                            Service_Foo(createdAt = "2022-03-09T05:01:50Z")
                        }.dataFetcher("successor") { env ->
                            if (env.getArgument<Any?>("after") == "2022-03-09T05:01:50Z") {
                                Service_Foo(id = "2023")
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
            },
        ),
    ),
) {
    private data class Service_Foo(
        val id: String? = null,
        val createdAt: String? = null,
        val successor: Service_Foo? = null,
    )
}
