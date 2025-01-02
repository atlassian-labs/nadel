package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `query with three nested hydrations and simple data` : NadelLegacyIntegrationTest(query
    = """
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
    |  bar: Bar
    |  barsById(id: [ID]): [Bar]
    |}
    |type Bar {
    |  barId: ID
    |  name: String
    |  nestedBar: Bar
    |  @hydrated(
    |    service: "Bar"
    |    field: "barsById"
    |    arguments: [{name: "id" value: "${'$'}source.nestedBarId"}]
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
    |type Query {
    |  bar: Bar
    |  barsById(id: [ID]): [Bar]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("barsById") { env ->
          if (env.getArgument<Any?>("id") == listOf("bar1")) {
            listOf(Bar_Bar(barId = "bar1", name = "Bar 1", nestedBarId = "nestedBar1"))}
          else if (env.getArgument<Any?>("id") == listOf("nestedBar1")) {
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
    |    field: "barsById"
    |    arguments: [{name: "id" value: "${'$'}source.barId"}]
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
          listOf(Foo_Foo(barId = "bar1"))}
      }
    }
    )
)) {
  private data class Bar_Bar(
    public val barId: String? = null,
    public val name: String? = null,
    public val nestedBarId: String? = null,
  )

  private data class Foo_Foo(
    public val barId: String? = null,
    public val name: String? = null,
  )
}
