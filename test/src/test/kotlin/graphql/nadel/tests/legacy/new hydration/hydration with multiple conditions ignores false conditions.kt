package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `hydration with multiple conditions ignores false conditions` :
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
    |union Bars = Bar
    |
    |type Query {
    |  barById(id: ID): Bar
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("barById") { env ->
          if (env.getArgument<Any?>("id") == "BAR_B") {
            Service2_Bar(name = "Bar B")}
          else {
            null}
        }
      }
      wiring.type("Bars") { type ->
        type.typeResolver { typeResolver ->
          val obj = typeResolver.getObject<Any>()
          val typeName = obj.javaClass.simpleName.substringAfter("_")
          typeResolver.schema.getTypeAs(typeName)
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
    |  bar: Bars
    |  @hydrated(
    |    service: "service2"
    |    field: "barById"
    |    arguments: [
    |      {
    |        name: "id" 
    |        value: "BAR_A"
    |      }
    |    ]
    |    when: {
    |      result: {
    |        sourceField: "type"
    |        predicate: { equals: "type_A" }
    |      }
    |    }
    |  )
    |  @hydrated(
    |    service: "service2" 
    |    field: "barById" 
    |    arguments: [
    |      {
    |        name: "id" 
    |        value: "BAR_B"
    |      }
    |    ]
    |    when: {
    |      result: {
    |        sourceField: "type"
    |        predicate: { equals: "type_B" }
    |      }
    |    }
    |  )
    |}
    |union Bars = Bar
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
          Service1_Foo(type = "type_B")}
      }
    }
    )
)) {
  private data class Service2_Bar(
    public val id: String? = null,
    public val name: String? = null,
    public val type: String? = null,
  ) : Service2_Bars

  private sealed interface Service2_Bars

  private data class Service1_Foo(
    public val barId: String? = null,
    public val id: String? = null,
    public val type: String? = null,
  )
}
