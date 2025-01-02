package graphql.nadel.tests.legacy.`chained transforms`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `two transforms on a field` : NadelLegacyIntegrationTest(query = """
|query {
|  foo {
|    id
|    epicEntity {
|      id
|      name
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service",
    overallSchema="""
    |directive @toBeDeleted on FIELD_DEFINITION
    |type Query {
    |  foo: Foo
    |}
    |type Foo {
    |  id: ID
    |  epicEntity: Epic @renamed(from: "epic") @toBeDeleted
    |}
    |type Epic {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  foo: Foo
    |}
    |
    |type Foo {
    |  id: ID
    |  epic: Epic
    |}
    |
    |type Epic {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          Service_Foo(id = "FOO-1")}
      }
    }
    )
)) {
  private data class Service_Epic(
    public val id: String? = null,
    public val name: String? = null,
  )

  private data class Service_Foo(
    public val id: String? = null,
    public val epic: Service_Epic? = null,
  )
}
