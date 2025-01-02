package graphql.nadel.tests.legacy.`new hydration`.`complex identified by`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `complex identified by uses same source for hydrations` :
    NadelLegacyIntegrationTest(query = """
|query {
|  foos {
|    issue {
|      field
|    }
|    detail {
|      name
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Foo", overallSchema="""
    |type Query {
    |  foos: [Foo]
    |  details(detailIds: [ID]): [Detail]
    |  issues(issueIds: [ID]): [Issue]
    |}
    |type Foo {
    |  issue: Issue @hydrated(
    |    service: "Foo"
    |    field: "issues"
    |    arguments: [{name: "issueIds" value: "${'$'}source.fooId"}]
    |    inputIdentifiedBy: [{sourceId: "fooId" resultId: "issueId"}]
    |    batchSize: 2
    |  )
    |  detail: Detail @hydrated(
    |    service: "Foo"
    |    field: "details"
    |    arguments: [{name: "detailIds" value: "${'$'}source.fooId"}]
    |    inputIdentifiedBy: [{sourceId: "fooId" resultId: "detailId"}]
    |    batchSize: 2
    |  )
    |}
    |type Detail {
    |  detailId: ID!
    |  name: String
    |}
    |type Issue {
    |  fooId: ID
    |  field: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Detail {
    |  detailId: ID!
    |  name: String
    |}
    |
    |type Foo {
    |  field: String
    |  fooId: ID
    |  issue: Issue
    |}
    |
    |type Issue {
    |  issueId: ID
    |  field: String
    |  fooId: ID
    |}
    |
    |type Query {
    |  details(detailIds: [ID]): [Detail]
    |  foos: [Foo]
    |  issues(issueIds: [ID]): [Issue]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foos") { env ->
          listOf(Foo_Foo(fooId = "Foo-1"), Foo_Foo(fooId = "Foo-2"), Foo_Foo(fooId = "Foo-3"))}

        .dataFetcher("issues") { env ->
          if (env.getArgument<Any?>("issueIds") == listOf("Foo-1", "Foo-2")) {
            listOf(Foo_Issue(`field` = "field_name", issueId = "Foo-1"), Foo_Issue(`field` =
                "field_name-2", issueId = "Foo-2"))}
          else if (env.getArgument<Any?>("issueIds") == listOf("Foo-3")) {
            listOf(Foo_Issue(`field` = "field-3", issueId = "Foo-3"))}
          else {
            null}
        }

        .dataFetcher("details") { env ->
          if (env.getArgument<Any?>("detailIds") == listOf("Foo-1", "Foo-2")) {
            listOf(Foo_Detail(detailId = "Foo-2", name = "Foo 2 Electric Boogaloo"),
                Foo_Detail(detailId = "Foo-1", name = "apple"))}
          else if (env.getArgument<Any?>("detailIds") == listOf("Foo-3")) {
            listOf(Foo_Detail(detailId = "Foo-3", name = "Three Apples"))}
          else {
            null}
        }
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
    public val fooId: String? = null,
    public val issue: Foo_Issue? = null,
  )

  private data class Foo_Issue(
    public val issueId: String? = null,
    public val `field`: String? = null,
    public val fooId: String? = null,
  )
}
