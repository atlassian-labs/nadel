package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars

public class `renaming json typed field` : NadelLegacyIntegrationTest(query = """
|query {
|  aField
|  test: aField
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  aField: JSON @renamed(from: "test")
    |}
    |scalar JSON
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  test: JSON
    |}
    |scalar JSON
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("test") { env ->
          if (env.field.resultKey == "rename__aField__test") {
            mapOf("object" to "aField")}
          else if (env.field.resultKey == "rename__test__test") {
            false}
          else {
            null}
        }
      }
      wiring.scalar(ExtendedScalars.Json)}
    )
))
