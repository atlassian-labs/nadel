package graphql.nadel.tests.legacy.namespaced

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Int
import kotlin.String

public class `query with  namespaced fields that have matching subfields` :
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
|  page {
|    getIssue {
|      pageText
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Issues",
    overallSchema="""
    |directive @namespaced on FIELD_DEFINITION
    |
    |type Query {
    |  issue: IssueQuery @namespaced
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
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  issue: IssueQuery
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
        type.dataFetcher("issue") { env ->
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
, Service(name="Pages", overallSchema="""
    |type Query {
    |  page: PagesQuery @namespaced
    |}
    |
    |type PagesQuery {
    |  getIssue: IssuePage
    |}
    |
    |type IssuePage {
    |  id: ID
    |  pageText: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  page: PagesQuery
    |}
    |
    |type PagesQuery {
    |  getIssue: IssuePage
    |}
    |
    |type IssuePage {
    |  id: ID
    |  pageText: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("page") { env ->
          Pages_PagesQuery(getIssue = Pages_IssuePage(pageText = "Bar"))}
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

  private data class IssueSearch_IssueQuery(
    public val search: IssueSearch_SearchResult? = null,
  )

  private data class IssueSearch_SearchResult(
    public val id: String? = null,
    public val count: Int? = null,
  )

  private data class Pages_IssuePage(
    public val id: String? = null,
    public val pageText: String? = null,
  )

  private data class Pages_PagesQuery(
    public val getIssue: Pages_IssuePage? = null,
  )
}
