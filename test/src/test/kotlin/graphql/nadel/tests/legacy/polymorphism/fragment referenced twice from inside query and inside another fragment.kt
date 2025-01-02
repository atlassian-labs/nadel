package graphql.nadel.tests.legacy.polymorphism

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `fragment referenced twice from inside query and inside another fragment` :
    NadelLegacyIntegrationTest(query = """
|query {
|  foo {
|    id
|    ...F2
|    ...F1
|  }
|}
|
|fragment F2 on Bar {
|  id
|}
|
|fragment F1 on Bar {
|  id
|  ...F2
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Foo", overallSchema="""
    |type Query {
    |  foo: Bar
    |}
    |type Bar {
    |  id: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Bar {
    |  id: String
    |}
    |
    |type Query {
    |  foo: Bar
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          Foo_Bar(id = "ID")}
      }
    }
    )
)) {
  private data class Foo_Bar(
    public val id: String? = null,
  )
}
