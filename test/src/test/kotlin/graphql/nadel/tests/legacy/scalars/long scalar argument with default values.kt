package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars
import graphql.scalars.alias.AliasedScalar
import java.math.BigInteger
import kotlin.Any

public class `long scalar argument with default values` : NadelLegacyIntegrationTest(query = """
|query {
|  getFoo(arg: 15)
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  getFoo(arg: Long = 123): String
    |}
    |scalar Long
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  getFoo(arg: Long = 123): String
    |}
    |scalar Long
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("getFoo") { env ->
          if (env.getArgument<BigInteger?>("arg")?.toInt() == 15) {
            "KFC Good"}
          else {
            null}
        }
      }
      wiring.scalar(AliasedScalar.Builder().name("Long").aliasedScalar(ExtendedScalars.Json).build())}
    )
))
