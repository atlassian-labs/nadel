package graphql.nadel.tests.legacy.`skip-include-fields`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `handles skip directive on single field whose parent returns object` :
    NadelLegacyIntegrationTest(query = """
|query (${'$'}test: Boolean!) {
|  foo {
|    id @skip(if: ${'$'}test)
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
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          Service_Foo()}
      }
    }
    )
)) {
  private data class Service_Foo(
    public val id: String? = null,
  )
}
