package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars
import graphql.scalars.alias.AliasedScalar

class `hydrating using url as arg` : NadelLegacyIntegrationTest(
    query = """
        query {
          foo {
            url
            details {
              baseUrl
              owner
              createdAt
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
                  lookup(url: URL): Lookup @hidden
                }
                type Foo {
                  id: ID
                  url: URL
                  details: Lookup
                  @hydrated(
                    service: "service"
                    field: "lookup"
                    arguments: [{ name: "url" value: "${'$'}source.url" }]
                  )
                }
                type Lookup {
                  baseUrl: URL
                  createdAt: DateTime
                  owner: String
                }
                scalar URL
                scalar DateTime
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  foo: Foo
                  lookup(url: URL): Lookup
                }
                type Foo {
                  id: ID
                  url: URL
                }
                type Lookup {
                  baseUrl: URL
                  createdAt: DateTime
                  owner: String
                }
                scalar URL
                scalar DateTime
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type
                        .dataFetcher("foo") { env ->
                            Service_Foo(url = "https://github.com/atlassian-labs/nadel")
                        }.dataFetcher("lookup") { env ->
                            if (env.getArgument<Any?>("url") == "https://github.com/atlassian-labs/nadel") {
                                Service_Lookup(
                                    baseUrl = "https://github.com/",
                                    createdAt = "2018-02-13T06:23:41Z",
                                    owner = "amarek",
                                )
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
        val id: String? = null,
        val url: String? = null,
    )

    private data class Service_Lookup(
        val baseUrl: String? = null,
        val createdAt: String? = null,
        val owner: String? = null,
    )
}
