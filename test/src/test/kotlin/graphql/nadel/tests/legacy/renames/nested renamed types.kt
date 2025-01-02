package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `nested renamed types` : NadelLegacyIntegrationTest(query = """
|query {
|  foo {
|    __typename
|    parent {
|      id
|      __typename
|      building {
|        __typename
|        id
|      }
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service1",
    overallSchema="""
    |type Query {
    |  foo: FooX
    |}
    |type FooX @renamed(from: "Foo") {
    |  id: ID
    |  parent: FooX
    |  building: Bar
    |}
    |type Bar @renamed(from: "Building") {
    |  id: ID!
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Foo {
    |  id: ID!
    |  parent: Foo
    |  building: Building
    |}
    |
    |type Building {
    |  id: ID!
    |}
    |
    |type Query {
    |  foo: Foo
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          Service1_Foo(parent = Service1_Foo(building = Service1_Building(id = "Bar-1"), id =
              "ParentFoo1"))}
      }
    }
    )
)) {
  private data class Service1_Building(
    public val id: String? = null,
  )

  private data class Service1_Foo(
    public val id: String? = null,
    public val parent: Service1_Foo? = null,
    public val building: Service1_Building? = null,
  )
}
