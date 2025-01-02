package graphql.nadel.tests.legacy.schema

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `can delete fields and types` : NadelLegacyIntegrationTest(query = """
|query GetTypes {
|  __schema {
|    types {
|      name
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  foo: Foo
    |  echo: String
    |}
    |type Foo {
    |  id: ID
    |}
    |type Bar {
    |  id: ID
    |  foo: Foo
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  foo: Foo
    |  echo: String
    |}
    |type Foo {
    |  id: ID
    |}
    |type Bar {
    |  id: ID
    |  foo: Foo
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
    }
    )
)) {
  private data class Service_Bar(
    public val id: String? = null,
    public val foo: Service_Foo? = null,
  )

  private data class Service_Foo(
    public val id: String? = null,
  )
}
