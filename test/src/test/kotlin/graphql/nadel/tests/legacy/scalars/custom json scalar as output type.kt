package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars

public class `custom json scalar as output type` : NadelLegacyIntegrationTest(query = """
|query {
|  aField
|  bField
|  cField: bField
|  dField: aField
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  aField: JSON
    |  bField: JSON
    |}
    |scalar JSON
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  aField: JSON
    |  bField: JSON
    |}
    |scalar JSON
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("aField") { env ->
          if (env.field.resultKey == "aField") {
            "1000"}
          else if (env.field.resultKey == "dField") {
            false}
          else {
            null}
        }

        .dataFetcher("bField") { env ->
          if (env.field.resultKey == "bField") {
            1_000}
          else if (env.field.resultKey == "cField") {
            mapOf("Something" to "Cool")}
          else {
            null}
        }
      }
      wiring.scalar(ExtendedScalars.Json)}
    )
))
