package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `basic hydration with static arg object` : NadelLegacyIntegrationTest(query = """
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
    |  barById(id: ID, fullName: FullName): Bar
    |}
    |type Bar {
    |  id: ID
    |  name: String
    |}
    |input FullName {
    |  firstName: String
    |  lastName: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Bar {
    |  id: ID
    |  name: String
    |}
    |
    |type Query {
    |  barById(id: ID, fullName: FullName): Bar
    |}
    |
    |input FullName {
    |  firstName: String
    |  lastName: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("barById") { env ->
          if (env.getArgument<Any?>("id") == "barId" && env.getArgument<Any?>("fullName") ==
              mapOf("firstName" to "first", "lastName" to "last")) {
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
    |  bar: Bar
    |  @hydrated(
    |    service: "service2"
    |    field: "barById"
    |    arguments: [
    |      { name: "id" value: "${'$'}source.id" }
    |      {
    |        name: "fullName"
    |        value: {
    |          firstName: "first"
    |          lastName: "last"
    |        }
    |      }
    |    ]
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
          Service1_Foo(id = "barId")}
      }
    }
    )
)) {
  private data class Service2_Bar(
    public val id: String? = null,
    public val name: String? = null,
  )

  private data class Service2_FullName(
    public val firstName: String? = null,
    public val lastName: String? = null,
  )

  private data class Service1_Foo(
    public val barId: String? = null,
    public val id: String? = null,
  )
}
