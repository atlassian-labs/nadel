package graphql.nadel.tests.legacy.`result merging`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

public class `not nullable top level field is absent` : NadelLegacyIntegrationTest(query = """
|query {
|  foo
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  foo: String!
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  foo: String!
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          null}
      }
    }
    )
))
