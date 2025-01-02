package graphql.nadel.tests.legacy.`new hydration`.batching

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.collections.List

public class `batching conditional hydration with list condition field` :
    NadelLegacyIntegrationTest(query = """
|query {
|  foo {
|    bar {
|        name
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service2",
    overallSchema="""
    |type Query {
    |  barsById(ids: [ID]): [Bar]
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
    |  barsById(ids: [ID]): [Bar]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("barsById") { env ->
          if (env.getArgument<Any?>("ids") == listOf("barId2")) {
            listOf(Service2_Bar(id = "barId2", name = "Bar2"))}
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
    |  barIds: [ID] @hidden
    |  bar: [Bar] 
    |    @hydrated(
    |      service: "service2"
    |      field: "barsById"
    |      arguments: [
    |        {
    |          name: "ids"
    |          value: "${'$'}source.barIds"
    |        }
    |      ]
    |      when: {
    |        result: {
    |          sourceField: "barIds"
    |          predicate: { equals: "barId2" }
    |        }
    |      }
    |    )
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
