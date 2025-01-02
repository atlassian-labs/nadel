package graphql.nadel.tests.legacy.namespaced

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Int
import kotlin.String

public class
    `query with two services sharing a namespaced field and a non namespaced top level field` :
    NadelLegacyIntegrationTest(query = """
|{
|  issue {
|    getIssue {
|      text
|    }
|
|    search {
|      count
|    }
|  }
|
|  conf {
|    title
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Issues",
    overallSchema="""
    |directive @namespaced on FIELD_DEFINITION
    |
    |type Query {
    |  issue: IssueQuery @namespaced
    |  conf: Page
    |}
    |
    |type IssueQuery {
    |  getIssue: Issue
    |}
    |
    |type Issue {
    |  id: ID
    |  text: String
    |}
    |
    |type Page {
    |  id: ID
    |  title: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  issue: IssueQuery
    |  conf: Page
    |}
    |
    |type Page {
    |  id: ID
    |  title: String
    |}
    |
    |type IssueQuery {
    |  getIssue: Issue
    |}
    |
    |type Issue {
    |  id: ID
    |  text: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("conf") { env ->
          Issues_Page(title = "Page title")}

        .dataFetcher("issue") { env ->
          Issues_IssueQuery(getIssue = Issues_Issue(text = "Foo"))}
      }
    }
    )
, Service(name="IssueSearch", overallSchema="""
    |extend type IssueQuery {
    |  search: SearchResult
    |}
    |
    |type SearchResult {
    |  id: ID
    |  count: Int
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  issue: IssueQuery
    |}
    |
    |type IssueQuery {
    |  search: SearchResult
    |}
    |
    |type SearchResult {
    |  id: ID
    |  count: Int
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issue") { env ->
          IssueSearch_IssueQuery(search = IssueSearch_SearchResult(count = 100))}
      }
    }
    )
)) {
  private data class Issues_Issue(
    public val id: String? = null,
    public val text: String? = null,
  )

  private data class Issues_IssueQuery(
    public val getIssue: Issues_Issue? = null,
  )

  private data class Issues_Page(
    public val id: String? = null,
    public val title: String? = null,
  )

  private data class IssueSearch_IssueQuery(
    public val search: IssueSearch_SearchResult? = null,
  )

  private data class IssueSearch_SearchResult(
    public val id: String? = null,
    public val count: Int? = null,
  )
}
