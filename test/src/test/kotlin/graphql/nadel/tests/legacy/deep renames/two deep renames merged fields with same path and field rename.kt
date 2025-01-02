package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `two deep renames merged fields with same path and field rename` :
    NadelLegacyIntegrationTest(query = """
|query {
|  issue {
|    id
|    authorId
|    authorName
|    details {
|      extra
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Issues",
    overallSchema="""
    |type Query {
    |  issue: Issue
    |}
    |type Issue {
    |  id: ID
    |  authorId: ID @renamed(from: "authorDetails.authorId")
    |  authorName: String @renamed(from: "authorDetails.name")
    |  details: AuthorDetail @renamed(from: "authorDetails")
    |}
    |type AuthorDetail {
    |  extra: String @renamed(from: "extraInfo")
    |}
    |""".trimMargin(), underlyingSchema="""
    |type AuthorDetail {
    |  authorId: ID
    |  extraInfo: String
    |  name: String
    |}
    |
    |type Issue {
    |  authorDetails: AuthorDetail
    |  id: ID
    |}
    |
    |type Query {
    |  issue: Issue
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issue") { env ->
          Issues_Issue(authorDetails = Issues_AuthorDetail(authorId = "USER-1", name = "User 1",
              extraInfo = "extra 1"), id = "ISSUE-1")}
      }
    }
    )
)) {
  private data class Issues_AuthorDetail(
    public val authorId: String? = null,
    public val extraInfo: String? = null,
    public val name: String? = null,
  )

  private data class Issues_Issue(
    public val authorDetails: Issues_AuthorDetail? = null,
    public val id: String? = null,
  )
}
