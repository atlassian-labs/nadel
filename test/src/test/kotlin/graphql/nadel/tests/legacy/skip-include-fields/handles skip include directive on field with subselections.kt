package graphql.nadel.tests.legacy.`skip-include-fields`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `handles skip include directive on field with subselections` :
    NadelLegacyIntegrationTest(query = """
|query (${'$'}test: Boolean!, ${'$'}invertTest: Boolean! = false, ${'$'}other: Boolean! = true) {
|  foo {
|    foo @skip(if: ${'$'}test) {
|      __typename @skip(if: ${'$'}invertTest)
|      id @include(if: ${'$'}test)
|    }
|    bar: foo @include(if: ${'$'}other) {
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
