package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

public class `hydration call with fragments in the hydrated part and synthetic field` :
    NadelLegacyIntegrationTest(query = """
|fragment IssueFragment on Issue {
|  id
|}
|
|query {
|  issues {
|    ...IssueFragment
|    id
|    authors {
|      id
|      ...UserFragment1
|    }
|  }
|  userQuery {
|    usersByIds(id: ["USER-1"]) {
|      ...UserFragment1
|    }
|  }
|}
|
|fragment UserFragment1 on User {
|  id
|  name
|  ...UserFragment2
|}
|
|fragment UserFragment2 on User {
|  name
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="UserService",
    overallSchema="""
    |type Query {
    |  userQuery: UserQuery
    |}
    |type UserQuery {
    |  usersByIds(id: [ID]): [User]
    |}
    |type User {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  userQuery: UserQuery
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
        type.dataFetcher("userQuery") {
          Unit}
      }
      wiring.type("UserQuery") { type ->
        type.dataFetcher("usersByIds") { env ->
          if (env.getArgument<Any?>("id") == listOf("USER-1")) {
            listOf(UserService_User(id = "USER-1", name = "User 1"))}
          else if (env.getArgument<Any?>("id") == listOf("USER-1", "USER-2")) {
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
    |  authorDetails: [AuthorDetail]
    |  authors: [User]
    |  @hydrated(
    |    service: "UserService"
    |    field: "userQuery.usersByIds"
    |    arguments: [{name: "id" value: "${'$'}source.authorDetails.authorId"}]
    |    identifiedBy: "id"
    |  )
    |}
    |type AuthorDetail {
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type AuthorDetail {
    |  authorId: ID
    |  name: String
    |}
    |
    |type Issue {
    |  authorDetails: [AuthorDetail]
    |  id: ID
    |}
    |
    |type Query {
    |  issues: [Issue]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issues") { env ->
          listOf(Issues_Issue(authorDetails = listOf(Issues_AuthorDetail(authorId = "USER-1"),
              Issues_AuthorDetail(authorId = "USER-2")), id = "ISSUE-1"))}
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

  private data class Issues_AuthorDetail(
    public val authorId: String? = null,
    public val name: String? = null,
  )

  private data class Issues_Issue(
    public val authorDetails: List<Issues_AuthorDetail?>? = null,
    public val id: String? = null,
  )
}
