package graphql.nadel.tests.legacy.namespaced

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Int
import kotlin.String
import kotlin.collections.List

public class
    `typename is sent owning service even when namespaced field and type are defined in different services`
    : NadelLegacyIntegrationTest(query = """
|{
|  issue {
|    __typename
|    getIssue {
|      text
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Issues",
    overallSchema="""
    |directive @namespaced on FIELD_DEFINITION
    |
    |type IssueQuery {
    |  getIssue: Issue
    |}
    |
    |extend type IssueQuery {
    |  getIssues: [Issue]
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
    |extend type IssueQuery {
    |  getIssues: [Issue]
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
    |type Query {
    |  issue: IssueQuery @namespaced
    |}
    |
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
    }
    )
, Service(name="IssueComments", overallSchema="""
    |extend type IssueQuery {
    |  commentsCount: Int
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  issue: IssueQuery
    |}
    |
    |type IssueQuery {
    |  commentsCount: Int
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
    }
    )
)) {
  private data class Issues_Issue(
    public val id: String? = null,
    public val text: String? = null,
  )

  private data class Issues_IssueQuery(
    public val getIssue: Issues_Issue? = null,
    public val getIssues: List<Issues_Issue?>? = null,
  )

  private data class IssueSearch_IssueQuery(
    public val search: IssueSearch_SearchResult? = null,
  )

  private data class IssueSearch_SearchResult(
    public val id: String? = null,
    public val count: Int? = null,
  )

  private data class IssueComments_IssueQuery(
    public val commentsCount: Int? = null,
  )
}
