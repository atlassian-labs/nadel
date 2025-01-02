package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars
import graphql.scalars.alias.AliasedScalar

public class `long scalar as output type` : NadelLegacyIntegrationTest(query = """
|query {
|  aField
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  aField: Long
    |}
    |scalar Long
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  aField: Long
    |}
    |scalar Long
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("aField") { env ->
          3_000_000_000}
      }
      wiring.scalar(AliasedScalar.Builder().name("Long").aliasedScalar(ExtendedScalars.Json).build())}
    )
))
