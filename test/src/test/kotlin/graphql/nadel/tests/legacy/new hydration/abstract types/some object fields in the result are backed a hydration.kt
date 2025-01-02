package graphql.nadel.tests.legacy.`new hydration`.`abstract types`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `some object fields in the result are backed a hydration` :
    NadelLegacyIntegrationTest(query = """
|{
|  activity {
|    user {
|      name
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="activity",
    overallSchema="""
    |type Query {
    |  activity: [IActivity]
    |}
    |interface IActivity {
    |  user: User
    |}
    |type Activity implements IActivity {
    |  id: ID!
    |  user: User
    |  @hydrated(
    |    service: "users"
    |    field: "userById"
    |    arguments: [{name: "id" value: "${'$'}source.userId"}]
    |  )
    |}
    |type SingleActivity implements IActivity {
    |  id: ID!
    |  user: User
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  activity: [IActivity]
    |}
    |type User {
    |  id: ID!
    |  name: String
    |}
    |interface IActivity {
    |  user: User
    |}
    |type Activity implements IActivity {
    |  id: ID!
    |  userId: ID
    |  user: User @deprecated(reason: "Fake")
    |}
    |type SingleActivity implements IActivity {
    |  id: ID!
    |  user: User
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("activity") { env ->
          listOf(Activity_SingleActivity(user = Activity_User(name = "John")),
              Activity_Activity(userId = "user-100"), Activity_Activity(userId = "user-20"),
              Activity_SingleActivity(user = Activity_User(name = "Mayor")))}
      }
      wiring.type("IActivity") { type ->
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
    |  userById(id: ID!): User
    |}
    |type User {
    |  id: ID!
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  userById(id: ID!): User
    |}
    |type User {
    |  id: ID!
    |  name: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("userById") { env ->
          if (env.getArgument<Any?>("id") == "user-100") {
            Users_User(name = "Hello")}
          else if (env.getArgument<Any?>("id") == "user-20") {
            Users_User(name = "World")}
          else {
            null}
        }
      }
    }
    )
)) {
  private data class Activity_Activity(
    public val id: String? = null,
    public val userId: String? = null,
    override val user: Activity_User? = null,
  ) : Activity_IActivity

  private interface Activity_IActivity {
    public val user: Activity_User?
  }

  private data class Activity_SingleActivity(
    public val id: String? = null,
    override val user: Activity_User? = null,
  ) : Activity_IActivity

  private data class Activity_User(
    public val id: String? = null,
    public val name: String? = null,
  )

  private data class Users_User(
    public val id: String? = null,
    public val name: String? = null,
  )
}
