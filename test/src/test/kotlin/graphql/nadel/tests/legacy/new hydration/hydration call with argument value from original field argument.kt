package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `hydration call with argument value from original field argument` :
    NadelLegacyIntegrationTest(query = """
|query {
|  issues {
|    id
|    author(extraArg: "extraArg") {
|      name
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="UserService",
    overallSchema="""
    |type Query {
    |  usersByIds(extraArg: String, id: [ID]): [User]
    |}
    |type User {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  usersByIds(extraArg: String, id: [ID]): [User]
    |}
    |
    |type User {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("usersByIds") { env ->
          if (env.getArgument<Any?>("extraArg") == "extraArg" && env.getArgument<Any?>("id") ==
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
    |  author(extraArg: String): User @hydrated(
    |    service: "UserService"
    |    field: "usersByIds"
    |    arguments: [
    |      {name: "extraArg" value: "${'$'}argument.extraArg"}
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

  private data class Issues_Issue(
    public val authorId: String? = null,
    public val id: String? = null,
  )
}
