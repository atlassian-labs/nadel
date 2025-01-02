package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars
import graphql.scalars.alias.AliasedScalar

public class `url scalar is passthrough and can be anything technically` :
    NadelLegacyIntegrationTest(query = """
|query {
|  aField
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  aField: URL
    |}
    |scalar URL
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  aField: URL
    |}
    |scalar URL
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("aField") { env ->
          "KFC Good"}
      }
      wiring.scalar(AliasedScalar.Builder().name("URL").aliasedScalar(ExtendedScalars.Json).build())}
    )
))
