package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars
import graphql.scalars.alias.AliasedScalar

public class `renaming url typed field` : NadelLegacyIntegrationTest(query = """
|query {
|  aField
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  aField: URL @renamed(from: "test")
    |}
    |scalar URL
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  test: URL
    |}
    |scalar URL
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("test") { env ->
          "https://github.com/atlassian-labs/nadel"}
      }
      wiring.scalar(AliasedScalar.Builder().name("URL").aliasedScalar(ExtendedScalars.Json).build())}
    )
))
