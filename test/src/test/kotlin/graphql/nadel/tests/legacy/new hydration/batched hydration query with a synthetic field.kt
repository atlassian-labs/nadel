package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

public class `batched hydration query with a synthetic field` : NadelLegacyIntegrationTest(query =
    """
|query {
|  issues {
|    id
|    authors {
|      id
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service2",
    overallSchema="""
    |type Query {
    |  users: UsersQuery
    |}
    |type UsersQuery {
    |  foo: String
    |  usersByIds(id: [ID]): [User]
    |}
    |type User {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  users: UsersQuery
    |}
    |
    |type User {
    |  id: ID
    |  name: String
    |}
    |
    |type UsersQuery {
    |  foo: String
    |  usersByIds(id: [ID]): [User]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("users") {
          Unit}
      }
      wiring.type("UsersQuery") { type ->
        type.dataFetcher("usersByIds") { env ->
          if (env.getArgument<Any?>("id") == listOf("USER-4", "USER-5")) {
            listOf(Service2_User(id = "USER-4"), Service2_User(id = "USER-5"))}
          else if (env.getArgument<Any?>("id") == listOf("USER-1", "USER-2", "USER-3")) {
            listOf(Service2_User(id = "USER-1"), Service2_User(id = "USER-2"), Service2_User(id =
                "USER-3"))}
          else {
            null}
        }
      }
    }
    )
, Service(name="service1", overallSchema="""
    |type Query {
    |  issues: [Issue]
    |}
    |type Issue {
    |  id: ID
    |  authors: [User]
    |  @hydrated(
    |    service: "service2"
    |    field: "users.usersByIds"
    |    arguments: [{name: "id" value: "${'$'}source.authorIds"}]
    |    identifiedBy: "id"
    |    batchSize: 3
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
          listOf(Service1_Issue(authorIds = listOf("USER-1", "USER-2"), id = "ISSUE-1"),
              Service1_Issue(authorIds = listOf("USER-3"), id = "ISSUE-2"), Service1_Issue(authorIds
              = listOf("USER-2", "USER-4", "USER-5"), id = "ISSUE-3"))}
      }
    }
    )
)) {
  private data class Service2_User(
    public val id: String? = null,
    public val name: String? = null,
  )

  private data class Service2_UsersQuery(
    public val foo: String? = null,
    public val usersByIds: List<Service2_User?>? = null,
  )

  private data class Service1_Issue(
    public val authorIds: List<String?>? = null,
    public val id: String? = null,
  )
}
