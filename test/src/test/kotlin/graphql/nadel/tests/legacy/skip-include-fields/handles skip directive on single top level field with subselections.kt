package graphql.nadel.tests.legacy.`skip-include-fields`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `handles skip directive on single top level field with subselections` :
    NadelLegacyIntegrationTest(query = """
|query (${'$'}test: Boolean!) {
|  foo @skip(if: ${'$'}test) {
|    id
|  }
|}
|""".trimMargin(), variables = mapOf("test" to true), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  foo: Foo
    |}
    |type Foo {
    |  id: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  foo: Foo
    |}
    |type Foo {
    |  id: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
    }
    )
)) {
  private data class Service_Foo(
    public val id: String? = null,
  )
}
