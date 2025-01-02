package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `hydration input is null` : NadelLegacyIntegrationTest(query = """
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
    |  author: User @hydrated(
    |    service: "UserService"
    |    field: "userById"
    |    arguments: [
    |      {name: "id" value: "${'$'}source.authorId"}
    |    ]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Issue {
    |  id: ID!
    |  authorId: ID
    |}
    |
    |type Query {
    |  issues: [Issue]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issues") { env ->
          listOf(Issues_Issue(authorId = null, id = "ISSUE-1"))}
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
    public val authorId: String? = null,
  )
}
