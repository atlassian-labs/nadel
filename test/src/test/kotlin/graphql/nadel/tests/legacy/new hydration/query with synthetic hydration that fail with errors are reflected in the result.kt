package graphql.nadel.tests.legacy.`new hydration`

import graphql.execution.DataFetcherResult
import graphql.nadel.engine.util.toGraphQLError
import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.Unit

public class `query with synthetic hydration that fail with errors are reflected in the result` :
    NadelLegacyIntegrationTest(query = """
|query {
|  foo {
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
    |  barQuery: BarQuery
    |}
    |type BarQuery {
    |  bar: Bar
    |  barById(id: ID): Bar
    |}
    |type Bar {
    |  name: String
    |  nestedBar: Bar
    |  @hydrated(
    |    service: "Bar"
    |    field: "barQuery.barById"
    |    arguments: [{name: "id" value: "${'$'}source.nestedBarId"}]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Bar {
    |  id: ID
    |  name: String
    |  nestedBarId: ID
    |}
    |
    |type BarQuery {
    |  bar: Bar
    |  barById(id: ID): Bar
    |}
    |
    |type Query {
    |  barQuery: BarQuery
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("barQuery") {
          Unit}
      }
      wiring.type("BarQuery") { type ->
        type.dataFetcher("barById") { env ->
          if (env.getArgument<Any?>("id") == "barId123") {
            DataFetcherResult.newResult<Any>().data(null).errors(listOf(toGraphQLError(mapOf("message"
                to "Error during hydration")))).build()}
          else {
            null}
        }
      }
    }
    )
, Service(name="Foo", overallSchema="""
    |type Query {
    |  foo: Foo
    |}
    |type Foo {
    |  name: String
    |  bar: Bar
    |  @hydrated(
    |    service: "Bar"
    |    field: "barQuery.barById"
    |    arguments: [{name: "id" value: "${'$'}source.barId"}]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Foo {
    |  barId: ID
    |  name: String
    |}
    |
    |type Query {
    |  foo: Foo
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          Foo_Foo(barId = "barId123")}
      }
    }
    )
)) {
  private data class Bar_Bar(
    public val id: String? = null,
    public val name: String? = null,
    public val nestedBarId: String? = null,
  )

  private data class Bar_BarQuery(
    public val bar: Bar_Bar? = null,
    public val barById: Bar_Bar? = null,
  )

  private data class Foo_Foo(
    public val barId: String? = null,
    public val name: String? = null,
  )
}
