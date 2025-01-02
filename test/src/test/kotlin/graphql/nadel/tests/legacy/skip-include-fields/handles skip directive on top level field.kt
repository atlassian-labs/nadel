package graphql.nadel.tests.legacy.`skip-include-fields`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

public class `handles skip directive on top level field` : NadelLegacyIntegrationTest(query = """
|query (${'$'}test: Boolean!) {
|  foo @skip(if: ${'$'}test)
|  bar @include(if: ${'$'}test)
|}
|""".trimMargin(), variables = mapOf("test" to true), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  foo: String
    |  bar: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  foo: String
    |  bar: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("bar") { env ->
          "Bar"}
      }
    }
    )
))
