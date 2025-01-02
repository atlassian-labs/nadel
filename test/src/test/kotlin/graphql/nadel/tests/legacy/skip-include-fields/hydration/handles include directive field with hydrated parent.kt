package graphql.nadel.tests.legacy.`skip-include-fields`.hydration

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `handles include directive field with hydrated parent` :
    NadelLegacyIntegrationTest(query = """
|query (${'$'}test: Boolean!) {
|  foo {
|    water {
|      id @include(if: ${'$'}test)
|    }
|  }
|}
|""".trimMargin(), variables = mapOf("test" to false), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  foo: Foo @renamed(from: "bar")
    |  fooById(id: ID): Foo
    |}
    |type Foo {
    |  id: String
    |  water: Foo @hydrated(
    |    service: "service"
    |    field: "fooById"
    |    arguments: [
    |      {name: "id" value: "${'$'}source.id"}
    |    ]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  bar: Foo
    |  fooById(id: ID): Foo
    |}
    |type Foo {
    |  id: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("bar") { env ->
          Service_Foo(id = "Foo-1")}

        .dataFetcher("fooById") { env ->
          if (env.getArgument<Any?>("id") == "Foo-1") {
            Service_Foo()}
          else {
            null}
        }
      }
    }
    )
)) {
  private data class Service_Foo(
    public val id: String? = null,
  )
}
