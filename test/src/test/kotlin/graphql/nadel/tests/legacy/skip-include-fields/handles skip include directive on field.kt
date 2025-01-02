package graphql.nadel.tests.legacy.`skip-include-fields`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `handles skip include directive on field` : NadelLegacyIntegrationTest(query = """
|query (${'$'}test: Boolean!) {
|  foo {
|    foo {
|      __typename @include(if: ${'$'}test)
|      id @skip(if: ${'$'}test)
|    }
|    bar: foo @include(if: ${'$'}test) {
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
    |  foo: Foo
    |  id: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          Service_Foo(foo = Service_Foo(id = "FOO-1"))}
      }
    }
    )
)) {
  private data class Service_Foo(
    public val foo: Service_Foo? = null,
    public val id: String? = null,
  )
}
