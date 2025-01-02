package graphql.nadel.tests.legacy.polymorphism

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `two top level fields with a fragment` : NadelLegacyIntegrationTest(query = """
|fragment I on Issue {
|  id
|}
|
|fragment U on User {
|  id
|  name
|}
|
|query {
|  issues {
|    ...I
|  }
|  user {
|    ...U
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="UserService",
    overallSchema="""
    |type Query {
    |  user: User
    |}
    |type User {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  user: User
    |}
    |
    |type User {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("user") { env ->
          UserService_User(id = "USER-1", name = "User 1")}
      }
    }
    )
, Service(name="Issues", overallSchema="""
    |type Query {
    |  issues: [Issue]
    |}
    |type Issue {
    |  id: ID
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
          listOf(Issues_Issue(id = "ISSUE-1"), Issues_Issue(id = "ISSUE-2"))}
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
