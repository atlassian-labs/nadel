package graphql.nadel.tests.legacy.`new hydration`.batching

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String
import kotlin.collections.List

public class `batching conditional hydration works when equals condition field is null` :
    NadelLegacyIntegrationTest(query = """
|query {
|  foo {
|    bar {
|      ... on Bar {
|        name
|      }
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
    }
    )
, Service(name="service1", overallSchema="""
    |type Query {
    |  foo: Foo
    |}
    |type Foo {
    |  id: ID
    |  type: String
    |  bar: [Bars] 
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
    |          sourceField: "type"
    |          predicate: { equals: "thatType" }
    |        }
    |      }
    |    )
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
    |          sourceField: "type"
    |          predicate: { equals: "thisType" }
    |        }
    |      }
    |    )
    |}
    |union Bars = Bar
    |""".trimMargin(), underlyingSchema="""
    |type Foo {
    |  barIds: [ID]
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
          Service1_Foo(barIds = listOf("barId1", "barId2"), type = null)}
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
    public val type: String? = null,
  )
}
