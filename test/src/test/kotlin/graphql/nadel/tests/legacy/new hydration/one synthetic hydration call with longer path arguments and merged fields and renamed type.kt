package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

public class
    `one synthetic hydration call with longer path arguments and merged fields and renamed type` :
    NadelLegacyIntegrationTest(query = """
|query {
|  issues {
|    id
|    authors {
|      name
|      id
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="UserService",
    overallSchema="""
    |type Query {
    |  usersQuery: RenamedUserQuery
    |}
    |type RenamedUserQuery @renamed(from: "UserQuery") {
    |  usersByIds(id: [ID]): [RenamedUser]
    |}
    |type RenamedUser @renamed(from: "User") {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  usersQuery: UserQuery
    |}
    |
    |type User {
    |  id: ID
    |  name: String
    |}
    |
    |type UserQuery {
    |  usersByIds(id: [ID]): [User]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("usersQuery") {
          Unit}
      }
      wiring.type("UserQuery") { type ->
        type.dataFetcher("usersByIds") { env ->
          if (env.getArgument<Any?>("id") == listOf("USER-1", "USER-2")) {
            listOf(UserService_User(id = "USER-1", name = "User 1"), UserService_User(id = "USER-2",
                name = "User 2"))}
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
    |  authors: [RenamedUser]
    |  @hydrated(
    |    service: "UserService"
    |    field: "usersQuery.usersByIds"
    |    arguments: [{name: "id" value: "${'$'}source.authors.authorId"}]
    |    identifiedBy: "id"
    |    batchSize: 2
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
              Issues_IssueUser(authorId = "USER-2")), id = "ISSUE-1"))}
      }
    }
    )
)) {
  private data class UserService_User(
    public val id: String? = null,
    public val name: String? = null,
  )

  private data class UserService_UserQuery(
    public val usersByIds: List<UserService_User?>? = null,
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
