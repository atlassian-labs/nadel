package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `deep rename of an object with transformations inside object` :
    NadelLegacyIntegrationTest(query = """
|query {
|  issues {
|    id
|    authorName {
|      firstName
|      lastName
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Issues",
    overallSchema="""
    |type Query {
    |  issues: [Issue]
    |}
    |type Issue {
    |  id: ID
    |  authorName: Name @renamed(from: "authorDetails.name")
    |}
    |type Name @renamed(from: "OriginalName") {
    |  firstName: String @renamed(from: "originalFirstName")
    |  lastName: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type AuthorDetail {
    |  name: OriginalName
    |}
    |
    |type Issue {
    |  authorDetails: AuthorDetail
    |  id: ID
    |}
    |
    |type OriginalName {
    |  lastName: String
    |  originalFirstName: String
    |}
    |
    |type Query {
    |  issues: [Issue]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issues") { env ->
          listOf(Issues_Issue(authorDetails = Issues_AuthorDetail(name =
              Issues_OriginalName(lastName = "Smith", originalFirstName = "George")), id =
              "ISSUE-1"), Issues_Issue(authorDetails = Issues_AuthorDetail(name =
              Issues_OriginalName(lastName = "Windsor", originalFirstName = "Elizabeth")), id =
              "ISSUE-2"))}
      }
    }
    )
)) {
  private data class Issues_AuthorDetail(
    public val name: Issues_OriginalName? = null,
  )

  private data class Issues_Issue(
    public val authorDetails: Issues_AuthorDetail? = null,
    public val id: String? = null,
  )

  private data class Issues_OriginalName(
    public val lastName: String? = null,
    public val originalFirstName: String? = null,
  )
}
