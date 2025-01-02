package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String
import kotlin.collections.List

public class `synthetic hydration input is empty list` : NadelLegacyIntegrationTest(query = """
|query {
|  issues {
|    id
|    authors {
|      name
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="UserService",
    overallSchema="""
    |type Query {
    |  usersQuery: UserQuery
    |}
    |type UserQuery {
    |  usersByIds(ids: [ID]): [User]
    |}
    |type User {
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
    |  usersByIds(ids: [ID]): [User]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
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
    |    field: "usersQuery.usersByIds"
    |    arguments: [{name: "ids" value: "${'$'}source.authorIds"}]
    |    identifiedBy: "id"
    |    batchSize: 2
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Issue {
    |  authorIds: [ID]
    |  id: ID
    |}
    |
    |type Query {
    |  issues: [Issue]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issues") { env ->
          listOf(Issues_Issue(authorIds = emptyList(), id = "ISSUE-1"))}
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
    public val id: String? = null,
  )
}
