package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public class `basic hydration with default object argument values` :
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
    |  barById(id: ID, test: Test): Bar
    |}
    |type Bar {
    |  id: ID
    |  name: String
    |}
    |input Test {
    |  string: String = "Test"
    |  int: Int = 42
    |  bool: Boolean = false
    |  number: Int
    |  echo: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Bar {
    |  id: ID
    |  name: String
    |}
    |
    |type Query {
    |  barById(id: ID, test: Test = {}): Bar
    |}
    |
    |input Test {
    |  string: String = "Test"
    |  int: Int = 42
    |  bool: Boolean = false
    |  number: Int
    |  echo: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("barById") { env ->
          if (env.getArgument<Any?>("id") == "barId" && env.getArgument<Any?>("test") ==
              mapOf("string" to "Test", "int" to 42, "bool" to false, "echo" to "Hello World")) {
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
    |  bar(test: Test = {echo: "Hello World"}): Bar @hydrated(
    |    service: "service2"
    |    field: "barById"
    |    arguments: [
    |      {name: "id" value: "${'$'}source.barId"}
    |      {name: "test" value: "${'$'}argument.test"}
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
          Service1_Foo(barId = "barId")}
      }
    }
    )
)) {
  private data class Service2_Bar(
    public val id: String? = null,
    public val name: String? = null,
  )

  private data class Service2_Test(
    public val string: String? = null,
    public val int: Int? = null,
    public val bool: Boolean? = null,
    public val number: Int? = null,
    public val echo: String? = null,
  )

  private data class Service1_Foo(
    public val barId: String? = null,
    public val id: String? = null,
  )
}
