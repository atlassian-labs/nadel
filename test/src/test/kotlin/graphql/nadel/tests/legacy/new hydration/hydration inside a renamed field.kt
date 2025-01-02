package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `hydration inside a renamed field` : NadelLegacyIntegrationTest(query = """
|query {
|  foo {
|    id
|    fooBar {
|      id
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Bar", overallSchema="""
    |type Query {
    |  barById(id: ID!): RenamedBar
    |}
    |type RenamedBar @renamed(from: "Bar") {
    |  id: ID!
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Bar {
    |  id: ID!
    |}
    |
    |type Query {
    |  barById(id: ID!): Bar
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("barById") { env ->
          if (env.getArgument<Any?>("id") == "hydrated-bar") {
            Bar_Bar(id = "hydrated-bar")}
          else {
            null}
        }
      }
    }
    )
, Service(name="Foo", overallSchema="""
    |type Query {
    |  foo: Foo @renamed(from: "fooOriginal")
    |}
    |type Foo {
    |  id: ID!
    |  fooBar: RenamedBar
    |  @hydrated(
    |    service: "Bar"
    |    field: "barById"
    |    arguments: [{name: "id" value: "${'$'}source.fooBarId"}]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Foo {
    |  fooBarId: ID
    |  id: ID!
    |}
    |
    |type Query {
    |  fooOriginal: Foo
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("fooOriginal") { env ->
          Foo_Foo(fooBarId = "hydrated-bar", id = "Foo")}
      }
    }
    )
)) {
  private data class Bar_Bar(
    public val id: String? = null,
  )

  private data class Foo_Foo(
    public val fooBarId: String? = null,
    public val id: String? = null,
  )
}
