package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars
import graphql.scalars.alias.AliasedScalar
import kotlin.Any
import kotlin.Long
import kotlin.String

public class `hydrating using long as arg` : NadelLegacyIntegrationTest(query = """
|query {
|  foo {
|    successor {
|      id
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  foo: Foo
    |  successor(after: Long): Foo
    |}
    |type Foo {
    |  id: ID
    |  createdAt: Long
    |  successor: Foo
    |  @hydrated(
    |    service: "service"
    |    field: "successor"
    |    arguments: [{ name: "after" value: "${'$'}source.createdAt" }]
    |  )
    |}
    |scalar Long
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  foo: Foo
    |  successor(after: Long): Foo
    |}
    |type Foo {
    |  id: ID
    |  createdAt: Long
    |  successor: Foo
    |}
    |scalar Long
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          Service_Foo(createdAt = 3_000_000_000)}

        .dataFetcher("successor") { env ->
          if (env.getArgument<Any?>("after") == 3_000_000_000) {
            Service_Foo(id = "2023")}
          else {
            null}
        }
      }
      wiring.scalar(AliasedScalar.Builder().name("Long").aliasedScalar(ExtendedScalars.Json).build())}
    )
)) {
  private data class Service_Foo(
    public val id: String? = null,
    public val createdAt: Long? = null,
    public val successor: Service_Foo? = null,
  )
}
