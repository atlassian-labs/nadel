package graphql.nadel.tests.legacy.`new hydration`.`complex identified by`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.collections.List

public class `complex identified by with some null source ids` : NadelLegacyIntegrationTest(query =
    """
|query {
|  issues {
|    id
|    authors {
|      id
|      name
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="UserService",
    overallSchema="""
    |type Query {
    |  users(id: [UserInput]): [User]
    |}
    |input UserInput {
    |  userId: ID
    |  site: String
    |}
    |type User {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  users(id: [UserInput]): [User]
    |}
    |
    |input UserInput {
    |  userId: ID
    |  site: String
    |}
    |
    |type User {
    |  id: ID
    |  name: String
    |  siteId: ID
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("users") { env ->
          if (env.getArgument<Any?>("id") == listOf(mapOf("userId" to "USER-2", "site" to "hello"),
              mapOf("userId" to "USER-3", "site" to "hello"), mapOf("userId" to "USER-2", "site" to
              "jdog"))) {
            listOf(UserService_User(id = "USER-2", name = "H-Two", siteId = "hello"),
                UserService_User(id = "USER-3", name = "H-Three", siteId = "hello"),
                UserService_User(id = "USER-2", name = "J-Two", siteId = "jdog"))}
          else if (env.getArgument<Any?>("id") == listOf(mapOf("userId" to "USER-5", "site" to
              "hello"))) {
            listOf(UserService_User(id = "USER-5", name = "H-Five", siteId = "hello"))}
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
    |  authors: [User] @hydrated(
    |    service: "UserService"
    |    field: "users"
    |    arguments: [{name: "id" value: "${'$'}source.authorIds"}]
    |    inputIdentifiedBy: [
    |      {sourceId: "authorIds.userId" resultId: "id"}
    |      {sourceId: "authorIds.site" resultId: "siteId"}
    |    ]
    |    batchSize: 3
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  issues: [Issue]
    |}
    |
    |type UserRef {
    |  userId: ID
    |  site: String
    |}
    |
    |type Issue {
    |  authorIds: [UserRef]
    |  id: ID
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issues") { env ->
          listOf(Issues_Issue(authorIds = listOf(null, Issues_UserRef(userId = "USER-2", site =
              "hello")), id = "ISSUE-1"), Issues_Issue(authorIds = listOf(Issues_UserRef(userId =
              "USER-3", site = "hello")), id = "ISSUE-2"), Issues_Issue(authorIds =
              listOf(Issues_UserRef(userId = "USER-2", site = "jdog"), null, Issues_UserRef(userId =
              "USER-5", site = "hello")), id = "ISSUE-3"))}
      }
    }
    )
)) {
  private data class UserService_User(
    public val id: String? = null,
    public val name: String? = null,
    public val siteId: String? = null,
  )

  private data class UserService_UserInput(
    public val userId: String? = null,
    public val site: String? = null,
  )

  private data class Issues_Issue(
    public val authorIds: List<Issues_UserRef?>? = null,
    public val id: String? = null,
  )

  private data class Issues_UserRef(
    public val userId: String? = null,
    public val site: String? = null,
  )
}
