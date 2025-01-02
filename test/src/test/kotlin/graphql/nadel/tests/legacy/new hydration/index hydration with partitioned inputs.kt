package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.collections.List

public class `index hydration with partitioned inputs` : NadelLegacyIntegrationTest(query = """
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
          if (env.getArgument<Any?>("id") == listOf("site-1/user-1", "site-1/user-3")) {
            listOf(UserService_User(id = "site-1/user-1"), UserService_User(id = "site-1/user-3"))}
          else if (env.getArgument<Any?>("id") == listOf("site-1/user-5")) {
            listOf(UserService_User(id = "site-1/user-5"))}
          else if (env.getArgument<Any?>("id") == listOf("site-2/user-2", "site-2/user-4")) {
            listOf(UserService_User(id = "site-2/user-2"), UserService_User(id = "site-2/user-4"))}
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
    |    arguments: [{name: "id" value: "${'$'}source.authorIds"}]
    |    identifiedBy: "id"
    |    batchSize: 2
    |    indexed: true
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
          listOf(Issues_Issue(authorIds = listOf("site-1/user-1", "site-2/user-2"), id = "ISSUE-1"),
              Issues_Issue(authorIds = listOf("site-1/user-3"), id = "ISSUE-2"),
              Issues_Issue(authorIds = listOf("site-2/user-4", "site-1/user-5"), id = "ISSUE-3"))}
      }
    }
    )
)) {
  private data class UserService_User(
    public val id: String? = null,
  )

  private data class Issues_Issue(
    public val authorIds: List<String?>? = null,
    public val id: String? = null,
  )
}
