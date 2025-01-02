package graphql.nadel.tests.legacy.`skip-include-fields`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `handles skip directive on single field with subselection` :
    NadelLegacyIntegrationTest(query = """
|query (${'$'}test: Boolean!) {
|  foo {
|    foo @skip(if: ${'$'}test) {
|      __typename
|      id
|    }
|  }
|}
|""".trimMargin(), variables = mapOf("test" to true), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  foo: Foo
    |}
    |type Foo {
    |  id: String
    |  foo: Foo
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  foo: Foo
    |}
    |type Foo {
    |  id: String
    |  foo: Foo
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
    public val foo: Service_Foo? = null,
  )
}
