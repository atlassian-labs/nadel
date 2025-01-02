package graphql.nadel.tests.legacy.scalars

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import graphql.scalars.ExtendedScalars
import kotlin.Any
import kotlin.String

public class `hydrating json data` : NadelLegacyIntegrationTest(query = """
|query {
|  foo(input: {something: true answer: "42"}) {
|    foo {
|      id
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  foo(input: JSON): Foo
    |}
    |type Foo {
    |  id: ID!
    |  foo: Foo @hydrated(
    |    service: "Baz"
    |    field: "otherFoo"
    |    arguments: [{ name: "id" value: "${'$'}source.id" }]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  foo(input: JSON): Foo
    |}
    |type Foo {
    |  id: ID!
    |}
    |scalar JSON
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          if (env.getArgument<Any?>("input") == mapOf("something" to true, "answer" to "42")) {
            Service_Foo(id = "10000")}
          else {
            null}
        }
      }
      wiring.scalar(ExtendedScalars.Json)}
    )
, Service(name="Baz", overallSchema="""
    |type Query {
    |  otherFoo(id: ID!): Foo @hidden
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  otherFoo(id: ID!): Foo
    |}
    |type Foo {
    |  id: ID!
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("otherFoo") { env ->
          if (env.getArgument<Any?>("id") == "10000") {
            Baz_Foo(id = "-10000")}
          else {
            null}
        }
      }
    }
    )
, Service(name="Shared", overallSchema="""
    |scalar JSON
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  echo: String
    |}
    |scalar JSON
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.scalar(ExtendedScalars.Json)}
    )
)) {
  private data class Service_Foo(
    public val id: String? = null,
  )

  private data class Baz_Foo(
    public val id: String? = null,
  )
}
