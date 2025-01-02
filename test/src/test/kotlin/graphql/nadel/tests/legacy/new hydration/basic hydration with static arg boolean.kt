package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.Boolean
import kotlin.String

public class `basic hydration with static arg boolean` : NadelLegacyIntegrationTest(query = """
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
    |  barWithSomeAttribute(someAttribute: Boolean): Bar
    |}
    |type Bar {
    |  id: ID
    |  name: String
    |  someAttribute: Boolean
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Bar {
    |  id: ID
    |  name: String
    |  someAttribute: Boolean
    |}
    |
    |type Query {
    |  barWithSomeAttribute(someAttribute: Boolean): Bar
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("barWithSomeAttribute") { env ->
          if (env.getArgument<Any?>("someAttribute") == true) {
            Service2_Bar(name = "Bar12345")}
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
    |  bar: Bar
    |  @hydrated(
    |    service: "service2"
    |    field: "barWithSomeAttribute"
    |    arguments: [{name: "someAttribute" value: true}]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Foo {
    |  barId: ID
    |  id: ID
    |}
    |
    |type Query {
    |  foo: Foo
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          Service1_Foo()}
      }
    }
    )
)) {
  private data class Service2_Bar(
    public val id: String? = null,
    public val name: String? = null,
    public val someAttribute: Boolean? = null,
  )

  private data class Service1_Foo(
    public val barId: String? = null,
    public val id: String? = null,
  )
}
