package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.collections.List

public class `batch hydration with renamed actor field` : NadelLegacyIntegrationTest(query = """
|query {
|  foo {
|    bar {
|      name
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service2",
    overallSchema="""
    |type Query {
    |  barsByIdOverall(id: [ID]): [Bar] @renamed(from: "barsById")
    |}
    |type Bar {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Bar {
    |  id: ID
    |  name: String
    |}
    |
    |type Query {
    |  barsById(id: [ID]): [Bar]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("barsById") { env ->
          if (env.getArgument<Any?>("id") == listOf("barId1", "barId2", "barId3")) {
            listOf(Service2_Bar(id = "barId1", name = "Bar1"), Service2_Bar(id = "barId2", name =
                "Bar2"), Service2_Bar(id = "barId3", name = "Bar3"))}
          else {
            null}
        }
      }
    }
    )
, Service(name="service1", overallSchema="""
    |type Query {
    |  foo: Foo
    |}
    |type Foo {
    |  id: ID
    |  bar: [Bar]
    |  @hydrated(
    |    service: "service2"
    |    field: "barsByIdOverall"
    |    arguments: [{name: "id" value: "${'$'}source.barId"}]
    |    identifiedBy: "id"
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Foo {
    |  barId: [ID]
    |  id: ID
    |}
    |
    |type Query {
    |  foo: Foo
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          Service1_Foo(barId = listOf("barId1", "barId2", "barId3"))}
      }
    }
    )
)) {
  private data class Service2_Bar(
    public val id: String? = null,
    public val name: String? = null,
  )

  private data class Service1_Foo(
    public val barId: List<String?>? = null,
    public val id: String? = null,
  )
}
