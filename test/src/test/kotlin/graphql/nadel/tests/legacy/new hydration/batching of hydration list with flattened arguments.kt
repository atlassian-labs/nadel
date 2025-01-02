package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.collections.List

public class `batching of hydration list with flattened arguments` :
    NadelLegacyIntegrationTest(query = """
|query {
|  issues {
|    id
|    authors {
|      id
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="UserService",
    overallSchema="""
    |type Query {
    |  usersByIds(id: [ID]): [User]
    |}
    |type User {
    |  id: ID
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  usersByIds(id: [ID]): [User]
    |}
    |
    |type User {
    |  id: ID
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("usersByIds") { env ->
          if (env.getArgument<Any?>("id") == listOf("USER-1", "USER-2", "USER-3")) {
            listOf(UserService_User(id = "USER-1"), UserService_User(id = "USER-2"),
                UserService_User(id = "USER-3"))}
          else if (env.getArgument<Any?>("id") == listOf("USER-4", "USER-5")) {
            listOf(UserService_User(id = "USER-4"), UserService_User(id = "USER-5"))}
          else {
            null}
        }
      }
    }
    )
, Service(name="Issues", overallSchema="""
    |type Query {
    |  issues: [Issue]
    |}
    |type Issue {
    |  id: ID
    |  authors: [User]
    |  @hydrated(
    |    service: "UserService"
    |    field: "usersByIds"
    |    arguments: [{name: "id" value: "${'$'}source.authors.authorId"}]
    |    identifiedBy: "id"
    |    batchSize: 3
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Issue {
    |  authorIds: [ID]
    |  authors: [IssueUser]
    |  id: ID
    |}
    |
    |type IssueUser {
    |  authorId: ID
    |}
    |
    |type Query {
    |  issues: [Issue]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issues") { env ->
          listOf(Issues_Issue(authors = listOf(Issues_IssueUser(authorId = "USER-1"),
              Issues_IssueUser(authorId = "USER-2")), id = "ISSUE-1"), Issues_Issue(authors =
              listOf(Issues_IssueUser(authorId = "USER-3")), id = "ISSUE-2"), Issues_Issue(authors =
              listOf(Issues_IssueUser(authorId = "USER-2"), Issues_IssueUser(authorId = "USER-4"),
              Issues_IssueUser(authorId = "USER-5")), id = "ISSUE-3"))}
      }
    }
    )
)) {
  private data class UserService_User(
    public val id: String? = null,
  )

  private data class Issues_Issue(
    public val authorIds: List<String?>? = null,
    public val authors: List<Issues_IssueUser?>? = null,
    public val id: String? = null,
  )

  private data class Issues_IssueUser(
    public val authorId: String? = null,
  )
}
