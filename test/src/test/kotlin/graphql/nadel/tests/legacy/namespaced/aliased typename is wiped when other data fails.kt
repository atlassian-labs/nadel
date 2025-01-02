package graphql.nadel.tests.legacy.namespaced

import graphql.execution.DataFetcherResult
import graphql.nadel.engine.util.toGraphQLError
import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.Int
import kotlin.String

public class `aliased typename is wiped when other data fails` : NadelLegacyIntegrationTest(query =
    """
|{
|  issue {
|    test: __typename
|    getIssue {
|      text
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
          DataFetcherResult.newResult<Any>().data(null).errors(listOf(toGraphQLError(mapOf("message"
              to "Error")))).build()}
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
}
