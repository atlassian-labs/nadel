package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `query to two services with field rename` : NadelLegacyIntegrationTest(query = """
|query {
|  otherFoo: foo {
|    name
|  }
|  bar {
|    name
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Bar", overallSchema="""
    |type Query {
    |  bar: Bar
    |}
    |type Bar {
    |  name: String @renamed(from: "title")
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Bar {
    |  title: String
    |}
    |
    |type Query {
    |  bar: Bar
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("bar") { env ->
          Bar_Bar(title = "Bar")}
      }
    }
    )
, Service(name="Foo", overallSchema="""
    |type Query {
    |  foo: Foo @renamed(from: "fooOriginal")
    |}
    |type Foo {
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Foo {
    |  name: String
    |}
    |
    |type Query {
    |  fooOriginal: Foo
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("fooOriginal") { env ->
          Foo_Foo(name = "Foo")}
      }
    }
    )
)) {
  private data class Bar_Bar(
    public val title: String? = null,
  )

  private data class Foo_Foo(
    public val name: String? = null,
  )
}
