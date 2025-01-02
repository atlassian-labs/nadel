package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `able to ask for field and use same field as hydration source` :
    NadelLegacyIntegrationTest(query = """
|query {
|  bar {
|    barId
|    nestedBar {
|      nestedBar {
|        barId
|      }
|      barId
|    }
|    name
|  }
|}
""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Bar", overallSchema="""
    |type Query {
    |  bar: Bar
    |  barById(id: ID): Bar
    |}
    |type Bar {
    |  barId: ID
    |  name: String
    |  nestedBar: Bar
    |  @hydrated(
    |    service: "Bar"
    |    field: "barById"
    |    arguments: [{name: "id" value: "${'$'}source.barId"}]
    |  )
    |}
    """.trimMargin(), underlyingSchema="""
    |type Bar {
    |  barId: ID
    |  name: String
    |}
    |
    |type Query {
    |  bar: Bar
    |  barById(id: ID): Bar
    |}
    """.trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("bar") { env ->
          Bar_Bar(barId = "1", name = "Test")}

        .dataFetcher("barById") { env ->
          if (env.getArgument<Any?>("id") == "1") {
            Bar_Bar(barId = "1")}
          else if (env.getArgument<Any?>("id") == "1") {
            Bar_Bar(barId = "1")}
          else {
            null}
        }
      }
    }
    )
)) {
  private data class Bar_Bar(
    public val barId: String? = null,
    public val name: String? = null,
  )
}
