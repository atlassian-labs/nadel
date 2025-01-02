package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.collections.List

public class `hydration list input` : NadelLegacyIntegrationTest(query = """
|query {
|  foo {
|    bar {
|      id
|      name
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service2",
    overallSchema="""
    |type Query {
    |  barById(id: ID): Bar
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
    |  barById(id: ID): Bar
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("barById") { env ->
          if (env.getArgument<Any?>("id") == "barId1") {
            Service2_Bar(id = "barId1", name = "Bar1")}
          else if (env.getArgument<Any?>("id") == "barId3") {
            Service2_Bar(id = "barId3", name = "Bar4")}
          else if (env.getArgument<Any?>("id") == "barId2") {
            Service2_Bar(id = "barId2", name = "Bar3")}
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
    |    field: "barById"
    |    arguments: [{name: "id" value: "${'$'}source.barIds"}]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Foo {
    |  barIds: [ID]
    |  id: ID
    |}
    |
    |type Query {
    |  foo: Foo
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          Service1_Foo(barIds = listOf("barId1", "barId2", "barId3"))}
      }
    }
    )
)) {
  private data class Service2_Bar(
    public val id: String? = null,
    public val name: String? = null,
  )

  private data class Service1_Foo(
    public val barIds: List<String?>? = null,
    public val id: String? = null,
  )
}
