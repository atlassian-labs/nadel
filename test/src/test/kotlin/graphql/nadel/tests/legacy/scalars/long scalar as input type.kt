package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars
import graphql.scalars.alias.AliasedScalar
import kotlin.Any

public class `long scalar as input type` : NadelLegacyIntegrationTest(query = """
|query {
|  foo(input: 3000000000) {
|    thing
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  foo(input: Long): Foo
    |}
    |type Foo {
    |  thing: JSON
    |}
    |scalar JSON
    |scalar Long
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  foo(input: Long): Foo
    |}
    |type Foo {
    |  thing: JSON
    |}
    |scalar JSON
    |scalar Long
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          if (env.getArgument<Any?>("input") == 3_000_000_000) {
            Service_Foo(thing = "What, were you expecting something else?")}
          else {
            null}
        }
      }
      wiring.scalar(ExtendedScalars.Json)
      wiring.scalar(AliasedScalar.Builder().name("Long").aliasedScalar(ExtendedScalars.Json).build())}
    )
)) {
  private data class Service_Foo(
    public val thing: Any? = null,
  )
}
