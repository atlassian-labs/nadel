package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.collections.List

public class `index hydration all null ids` : NadelLegacyIntegrationTest(query = """
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
    |    indexed: true
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
            Issues_Issue(collaboratorIds = listOf(null, null), key = "GQLGW-1000")}
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
