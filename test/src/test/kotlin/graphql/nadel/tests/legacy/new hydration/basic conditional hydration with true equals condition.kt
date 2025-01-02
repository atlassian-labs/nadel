package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `basic conditional hydration with true equals condition` :
    NadelLegacyIntegrationTest(query = """
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
    |  barById(id: ID): Bar
    |}
    |type Bar {
    |  id: ID
    |  name: String
    |  type: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Bar {
    |  id: ID
    |  name: String
    |  type: String
    |}
    |
    |type Query {
    |  barById(id: ID): Bar
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("barById") { env ->
          if (env.getArgument<Any?>("id") == "barId") {
            Service2_Bar(name = "Bar1")}
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
    |  type: String
    |  bar: Bar @hydrated(
    |    service: "service2" 
    |    field: "barById" 
    |    arguments: [
    |      {
    |        name: "id" 
    |        value: "${'$'}source.barId"
    |      }
    |    ]
    |    when: {
    |      result: {
    |        sourceField: "type"
    |        predicate: { equals: "someType" }
    |      }
    |    }
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Foo {
    |  barId: ID
    |  id: ID
    |  type: String
    |}
    |
    |type Query {
    |  foo: Foo
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          Service1_Foo(barId = "barId", type = "someType")}
      }
    }
    )
)) {
  private data class Service2_Bar(
    public val id: String? = null,
    public val name: String? = null,
    public val type: String? = null,
  )

  private data class Service1_Foo(
    public val barId: String? = null,
    public val id: String? = null,
    public val type: String? = null,
  )
}
