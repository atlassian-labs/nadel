package graphql.nadel.tests.legacy.basic

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

public class `can generate legacy operation names forwarding original name` :
    NadelLegacyIntegrationTest(query = """
|query Original {
|  foo
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="test", overallSchema="""
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
          null}
      }
    }
    )
))
