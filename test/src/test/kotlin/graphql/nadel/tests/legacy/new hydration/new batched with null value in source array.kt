package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.collections.List

public class `new batched with null value in source array` : NadelLegacyIntegrationTest(query = """
|query {
|  issueById(id: "10000") {
|    key
|    collaborators {
|      __typename
|      name
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Issues",
    overallSchema="""
    |type Query {
    |  issueById(id: ID!): Issue!
    |}
    |type Issue {
    |  id: ID!
    |  key: String
    |  collaborators: [User]
    |  @hydrated(
    |    service: "Users"
    |    field: "usersByIds"
    |    arguments: [
    |      {name: "ids", value: "${'$'}source.collaboratorIds"}
    |    ]
    |    identifiedBy: "id"
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  issueById(id: ID!): Issue!
    |}
    |type Issue {
    |  id: ID!
    |  key: String
    |  collaboratorIds: [ID]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issueById") { env ->
          if (env.getArgument<Any?>("id") == "10000") {
            Issues_Issue(collaboratorIds = listOf("100", null, "200"), key = "GQLGW-1000")}
          else {
            null}
        }
      }
    }
    )
, Service(name="Users", overallSchema="""
    |type Query {
    |  usersByIds(ids: [ID!]!): [User]
    |}
    |type User {
    |  id: ID!
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  usersByIds(ids: [ID!]!): [User]
    |}
    |type User {
    |  id: ID!
    |  name: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("usersByIds") { env ->
          if (env.getArgument<Any?>("ids") == listOf("100", "200")) {
            listOf(Users_User(id = "100", name = "John Doe"), Users_User(id = "200", name = "Joe"))}
          else {
            null}
        }
      }
    }
    )
)) {
  private data class Issues_Issue(
    public val id: String? = null,
    public val key: String? = null,
    public val collaboratorIds: List<String?>? = null,
  )

  private data class Users_User(
    public val id: String? = null,
    public val name: String? = null,
  )
}
