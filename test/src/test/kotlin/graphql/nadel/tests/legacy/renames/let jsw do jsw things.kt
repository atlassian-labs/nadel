package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars
import graphql.scalars.alias.AliasedScalar

public class `let jsw do jsw things` : NadelLegacyIntegrationTest(query = """
|query {
|  foo
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  foo: A
    |}
    |
    |scalar A @renamed(from: "X")
    |scalar B @renamed(from: "X")
    |
    |scalar C @renamed(from: "Y")
    |scalar D @renamed(from: "Y")
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  foo: X
    |}
    |scalar X
    |enum Y {
    |  P
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          "Custom Scalar"}
      }
      wiring.scalar(AliasedScalar.Builder().name("X").aliasedScalar(ExtendedScalars.Json).build())}
    )
)) {
  private enum class Service_Y {
    P,
  }
}
