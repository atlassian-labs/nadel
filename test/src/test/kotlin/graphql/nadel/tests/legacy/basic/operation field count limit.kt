package graphql.nadel.tests.legacy.basic

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `operation field count limit` : NadelLegacyIntegrationTest(query = """
|query {
|  foo {
|    __typename
|    name
|    child {
|      name
|    }
|  }
|  __typename
|  bar: foo {
|      barName: name
|      barChild: child {
|        barTypeName: __typename
|        name
|      }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  foo: Foo
    |}
    |type Foo {
    |  name: String
    |  child: Foo
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  foo: Foo
    |}
    |type Foo {
    |  name: String
    |  child: Foo
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
    }
    )
)) {
  private data class Service_Foo(
    public val name: String? = null,
    public val child: Service_Foo? = null,
  )
}
