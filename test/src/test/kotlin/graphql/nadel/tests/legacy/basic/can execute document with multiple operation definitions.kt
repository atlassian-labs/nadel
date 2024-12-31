package graphql.nadel.tests.legacy.basic

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

public class `can execute document with multiple operation definitions` :
    NadelLegacyIntegrationTest(operationName = "Test", query = """
|query Foo {
|  foo
|}
|query Test {
|  test: foo
|}
|query Dog {
|  dog: foo
|}
|query Meow {
|  cat: foo
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  foo: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  foo: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          "Test Working"}
      }
    }
    )
))
