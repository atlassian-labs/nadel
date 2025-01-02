package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String
import kotlin.collections.List

public class `hydration input is empty list` : NadelLegacyIntegrationTest(query = """
|query {
|  issues {
|    id
|    author {
|      name
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="UserService",
    overallSchema="""
    |type Query {
    |  userById(id: ID!): User
    |}
    |type User {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  userById(id: ID!): User
    |}
    |
    |type User {
    |  id: ID
    |  name: String
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
    |  author: [User] @hydrated(
    |    service: "UserService"
    |    field: "userById"
    |    arguments: [
    |      {name: "id" value: "${'$'}source.authorIds"}
    |    ]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Issue {
    |  id: ID!
    |  authorIds: [ID]
    |}
    |
    |type Query {
    |  issues: [Issue]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issues") { env ->
          listOf(Issues_Issue(authorIds = null, id = "ISSUE-1"))}
      }
    }
    )
)) {
  private data class UserService_User(
    public val id: String? = null,
    public val name: String? = null,
  )

  private data class Issues_Issue(
    public val id: String? = null,
    public val authorIds: List<String?>? = null,
  )
}
