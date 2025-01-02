package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars
import graphql.scalars.alias.AliasedScalar
import kotlin.Any
import kotlin.String

public class `hydrating using url as arg` : NadelLegacyIntegrationTest(query = """
|query {
|  foo {
|    url
|    details {
|      baseUrl
|      owner
|      createdAt
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  foo: Foo
    |  lookup(url: URL): Lookup @hidden
    |}
    |type Foo {
    |  id: ID
    |  url: URL
    |  details: Lookup
    |  @hydrated(
    |    service: "service"
    |    field: "lookup"
    |    arguments: [{ name: "url" value: "${'$'}source.url" }]
    |  )
    |}
    |type Lookup {
    |  baseUrl: URL
    |  createdAt: DateTime
    |  owner: String
    |}
    |scalar URL
    |scalar DateTime
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  foo: Foo
    |  lookup(url: URL): Lookup
    |}
    |type Foo {
    |  id: ID
    |  url: URL
    |}
    |type Lookup {
    |  baseUrl: URL
    |  createdAt: DateTime
    |  owner: String
    |}
    |scalar URL
    |scalar DateTime
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          Service_Foo(url = "https://github.com/atlassian-labs/nadel")}

        .dataFetcher("lookup") { env ->
          if (env.getArgument<Any?>("url") == "https://github.com/atlassian-labs/nadel") {
            Service_Lookup(baseUrl = "https://github.com/", createdAt = "2018-02-13T06:23:41Z",
                owner = "amarek")}
          else {
            null}
        }
      }
      wiring.scalar(AliasedScalar.Builder().name("DateTime").aliasedScalar(ExtendedScalars.Json).build())
      wiring.scalar(AliasedScalar.Builder().name("URL").aliasedScalar(ExtendedScalars.Json).build())}
    )
)) {
  private data class Service_Foo(
    public val id: String? = null,
    public val url: String? = null,
  )

  private data class Service_Lookup(
    public val baseUrl: String? = null,
    public val createdAt: String? = null,
    public val owner: String? = null,
  )
}
