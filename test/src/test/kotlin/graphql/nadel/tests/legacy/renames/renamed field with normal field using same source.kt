package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `renamed field with normal field using same source` : NadelLegacyIntegrationTest(query
    = """
|query {
|  foo {
|    issue {
|      fooDetail {
|        name
|      }
|    }
|    renamedField
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Foo", overallSchema="""
    |type Query {
    |  foo: Foo
    |}
    |type Foo {
    |  renamedField: String @renamed(from: "issue.field")
    |  issue: Issue
    |}
    |type Issue {
    |  fooDetail: Detail
    |}
    |type Detail {
    |  detailId: ID!
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Detail {
    |  detailId: ID!
    |  name: String
    |}
    |
    |type Foo {
    |  field: String
    |  issue: Issue
    |}
    |
    |type Issue {
    |  field: String
    |  fooDetail: Detail
    |}
    |
    |type Query {
    |  detail(detailIds: [ID]): Detail
    |  foo: Foo
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          Foo_Foo(issue = Foo_Issue(fooDetail = Foo_Detail(name = "fooName"), `field` = "field"))}
      }
    }
    )
)) {
  private data class Foo_Detail(
    public val detailId: String? = null,
    public val name: String? = null,
  )

  private data class Foo_Foo(
    public val `field`: String? = null,
    public val issue: Foo_Issue? = null,
  )

  private data class Foo_Issue(
    public val `field`: String? = null,
    public val fooDetail: Foo_Detail? = null,
  )
}
