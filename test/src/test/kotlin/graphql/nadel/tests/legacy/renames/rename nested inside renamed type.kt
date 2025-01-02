package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `rename nested inside renamed type` : NadelLegacyIntegrationTest(query = """
|query {
|  foo {
|    __typename
|    parent {
|      title
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service1",
    overallSchema="""
    |type Query {
    |  foo: FooX
    |}
    |type FooX @renamed(from: "Foo") {
    |  id: ID
    |  title: ID @renamed(from: "barId")
    |  parent: FooX
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Foo {
    |  barId: ID
    |  id: ID
    |  parent: Foo
    |}
    |
    |type Query {
    |  foo: Foo
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          Service1_Foo(parent = Service1_Foo(barId = "Foo1-2"))}
      }
    }
    )
)) {
  private data class Service1_Foo(
    public val barId: String? = null,
    public val id: String? = null,
    public val parent: Service1_Foo? = null,
  )
}
