package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `hydration from field in interface` : NadelLegacyIntegrationTest(query = """
|query {
|  issue(id: "1") {
|    title
|    issueAuthor {
|      name
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="issues",
    overallSchema="""
    |type Query {
    |  issue(id: ID): Issue
    |}
    |type Issue {
    |  id: ID
    |  title: String
    |  issueAuthor: User
    |  @hydrated(
    |    service: "users"
    |    field: "user"
    |    arguments: [{name: "id" value: "${'$'}source.author.userId"}]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Issue {
    |  id: ID!
    |  title: String
    |  author: User
    |}
    |
    |interface User {
    |  userId: ID!
    |}
    |
    |type ProductUser implements User {
    |  userId: ID!
    |  email: String
    |}
    |
    |type Query {
    |  issue(id: ID): Issue
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issue") { env ->
          if (env.getArgument<Any?>("id") == "1") {
            Issues_Issue(author = Issues_ProductUser(userId = "1001"), title = "Issue 1")}
          else {
            null}
        }
      }
      wiring.type("User") { type ->
        type.typeResolver { typeResolver ->
          val obj = typeResolver.getObject<Any>()
          val typeName = obj.javaClass.simpleName.substringAfter("_")
          typeResolver.schema.getTypeAs(typeName)
        }
      }
    }
    )
, Service(name="users", overallSchema="""
    |type Query {
    |  user(id: ID!): User
    |}
    |type User {
    |  id: ID!
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type User {
    |  id: ID!
    |  name: String
    |}
    |
    |type Query {
    |  user(id: ID!): User
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("user") { env ->
          if (env.getArgument<Any?>("id") == "1001") {
            Users_User(name = "McUser Face")}
          else {
            null}
        }
      }
    }
    )
)) {
  private data class Issues_Issue(
    public val id: String? = null,
    public val title: String? = null,
    public val author: Issues_User? = null,
  )

  private data class Issues_ProductUser(
    override val userId: String? = null,
    public val email: String? = null,
  ) : Issues_User

  private interface Issues_User {
    public val userId: String?
  }

  private data class Users_User(
    public val id: String? = null,
    public val name: String? = null,
  )
}
