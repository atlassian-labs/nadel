package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

public class `synthetic hydration batching returns null` : NadelLegacyIntegrationTest(query = """
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
    |  barsQuery: BarsQuery
    |}
    |type BarsQuery {
    |  barsById(id: [ID]): [Bar]
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
    |type BarsQuery {
    |  barsById(id: [ID]): [Bar]
    |}
    |
    |type Query {
    |  barsQuery: BarsQuery
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("barsQuery") {
          Unit}
      }
      wiring.type("BarsQuery") { type ->
        type.dataFetcher("barsById") { env ->
          if (env.getArgument<Any?>("id") == listOf("barId1", "barId2", "barId3")) {
            null}
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
    |    field: "barsQuery.barsById"
    |    arguments: [{name: "id" value: "${'$'}source.barId"}]
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

  private data class Service2_BarsQuery(
    public val barsById: List<Service2_Bar?>? = null,
  )

  private data class Service1_Foo(
    public val barId: List<String?>? = null,
    public val id: String? = null,
  )
}
