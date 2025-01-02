package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

public class `synthetic hydration call with two argument values from original field arguments` :
    NadelLegacyIntegrationTest(query = """
|query {
|  issues {
|    id
|    author(extraArg1: "extraArg1", extraArg2: 10) {
|      name
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="UserService",
    overallSchema="""
    |type Query {
    |  usersQuery: UsersQuery
    |}
    |type UsersQuery {
    |  usersByIds(extraArg1: String, extraArg2: Int, id: [ID]): [User]
    |}
    |type User {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  usersQuery: UsersQuery
    |}
    |
    |type User {
    |  id: ID
    |  name: String
    |}
    |
    |type UsersQuery {
    |  usersByIds(extraArg1: String, extraArg2: Int, id: [ID]): [User]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("usersQuery") {
          Unit}
      }
      wiring.type("UsersQuery") { type ->
        type.dataFetcher("usersByIds") { env ->
          if (env.getArgument<Any?>("extraArg1") == "extraArg1" &&
              env.getArgument<Any?>("extraArg2") == 10 && env.getArgument<Any?>("id") ==
              listOf("USER-1")) {
            listOf(UserService_User(id = "USER-1", name = "User 1"))}
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
    |  author(extraArg1: String, extraArg2: Int): User @hydrated(
    |    service: "UserService"
    |    field: "usersQuery.usersByIds"
    |    arguments: [
    |      {name: "extraArg1" value: "${'$'}argument.extraArg1"}
    |      {name: "extraArg2" value: "${'$'}argument.extraArg2"}
    |      {name: "id" value: "${'$'}source.authorId"}
    |    ]
    |    identifiedBy: "id"
    |    batchSize: 2
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Issue {
    |  authorId: ID
    |  id: ID
    |}
    |
    |type Query {
    |  issues: [Issue]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issues") { env ->
          listOf(Issues_Issue(authorId = "USER-1", id = "ISSUE-1"))}
      }
    }
    )
)) {
  private data class UserService_User(
    public val id: String? = null,
    public val name: String? = null,
  )

  private data class UserService_UsersQuery(
    public val usersByIds: List<UserService_User?>? = null,
  )

  private data class Issues_Issue(
    public val authorId: String? = null,
    public val id: String? = null,
  )
}
