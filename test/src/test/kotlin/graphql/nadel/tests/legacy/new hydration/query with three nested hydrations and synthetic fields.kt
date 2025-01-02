package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

public class `query with three nested hydrations and synthetic fields` :
    NadelLegacyIntegrationTest(query = """
|query {
|  foos {
|    bar {
|      name
|      nestedBar {
|        name
|        nestedBar {
|          name
|        }
|      }
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Bar", overallSchema="""
    |type Query {
    |  barsQuery: BarQuery
    |}
    |type BarQuery {
    |  bar: Bar
    |  barsById(id: [ID]): [Bar]
    |}
    |type Bar {
    |  barId: ID
    |  name: String
    |  nestedBar: Bar
    |  @hydrated(
    |    service: "Bar"
    |    field: "barsQuery.barsById"
    |    arguments: [{ name: "id" value: "${'$'}source.nestedBarId"}]
    |    identifiedBy: "barId"
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Bar {
    |  barId: ID
    |  name: String
    |  nestedBarId: ID
    |}
    |
    |type BarQuery {
    |  bar: Bar
    |  barsById(id: [ID]): [Bar]
    |}
    |
    |type Query {
    |  barsQuery: BarQuery
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("barsQuery") {
          Unit}
      }
      wiring.type("BarQuery") { type ->
        type.dataFetcher("barsById") { env ->
          if (env.getArgument<Any?>("id") == listOf("bar3")) {
            listOf(Bar_Bar(barId = "bar3", name = "Bar 3", nestedBarId = null))}
          else if (env.getArgument<Any?>("id") == listOf("bar1", "bar2")) {
            listOf(Bar_Bar(barId = "bar1", name = "Bar 1", nestedBarId = "nestedBar1"),
                Bar_Bar(barId = "bar2", name = "Bar 2", nestedBarId = "nestedBar2"))}
          else if (env.getArgument<Any?>("id") == listOf("nestedBar1", "nestedBar2")) {
            listOf(Bar_Bar(barId = "nestedBar1", name = "NestedBarName1", nestedBarId =
                "nestedBarId456"))}
          else if (env.getArgument<Any?>("id") == listOf("nestedBarId456")) {
            listOf(Bar_Bar(barId = "nestedBarId456", name = "NestedBarName2"))}
          else {
            null}
        }
      }
    }
    )
, Service(name="Foo", overallSchema="""
    |type Query {
    |  foos: [Foo]
    |}
    |type Foo {
    |  name: String
    |  bar: Bar
    |  @hydrated(
    |    service: "Bar"
    |    field: "barsQuery.barsById"
    |    arguments: [{ name: "id" value: "${'$'}source.barId"}]
    |    identifiedBy: "barId"
    |    batchSize: 2
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Foo {
    |  barId: ID
    |  name: String
    |}
    |
    |type Query {
    |  foos: [Foo]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foos") { env ->
          listOf(Foo_Foo(barId = "bar1"), Foo_Foo(barId = "bar2"), Foo_Foo(barId = "bar3"))}
      }
    }
    )
)) {
  private data class Bar_Bar(
    public val barId: String? = null,
    public val name: String? = null,
    public val nestedBarId: String? = null,
  )

  private data class Bar_BarQuery(
    public val bar: Bar_Bar? = null,
    public val barsById: List<Bar_Bar?>? = null,
  )

  private data class Foo_Foo(
    public val barId: String? = null,
    public val name: String? = null,
  )
}
