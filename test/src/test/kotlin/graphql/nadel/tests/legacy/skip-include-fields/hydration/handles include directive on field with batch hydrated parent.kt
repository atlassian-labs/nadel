package graphql.nadel.tests.legacy.`skip-include-fields`.hydration

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `handles include directive on field with batch hydrated parent` :
    NadelLegacyIntegrationTest(query = """
|query (${'$'}test: Boolean!) {
|  foos {
|    test {
|      id @include(if: ${'$'}test)
|    }
|  }
|}
|""".trimMargin(), variables = mapOf("test" to false), services = listOf(Service(name="service",
    overallSchema="""
    |type Query {
    |  foos: [Foo]
    |  tests(ids: [ID]): [Test]
    |}
    |type Foo {
    |  test: Test @hydrated(
    |    service: "service"
    |    field: "tests"
    |    arguments: [
    |      {name: "ids" value: "${'$'}source.id"}
    |    ]
    |    identifiedBy: "id"
    |  )
    |}
    |type Test {
    |  id: ID
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  foos: [Foo]
    |  tests(ids: [ID]): [Test]
    |}
    |type Foo {
    |  id: String
    |}
    |type Test {
    |  id: ID
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foos") { env ->
          listOf(Service_Foo(id = "Foo-3"), Service_Foo(id = "Foo-4"))}

        .dataFetcher("tests") { env ->
          if (env.getArgument<Any?>("ids") == listOf("Foo-3", "Foo-4")) {
              listOf(Service_Test(id = "Foo-4"), Service_Test(id = "Foo-3"))
          }
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

  private data class Service_Test(
    public val id: String? = null,
  )
}
