package graphql.nadel.tests.legacy.`new hydration`.index

import graphql.execution.DataFetcherResult
import graphql.nadel.engine.util.toGraphQLError
import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.collections.List

public class `hydration matching using index one batch returns errors` :
    NadelLegacyIntegrationTest(query = """
|query {
|  issues {
|    id
|    authors {
|      name
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="UserService",
    overallSchema="""
    |type Query {
    |  usersByIds(ids: [ID]): [User]
    |}
    |type User {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  usersByIds(ids: [ID]): [User]
    |}
    |type User {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("usersByIds") { env ->
          if (env.getArgument<Any?>("ids") == listOf("1", "2")) {
            listOf(UserService_User(name = "User-1"), null)}
          else if (env.getArgument<Any?>("ids") == listOf("4")) {
            DataFetcherResult.newResult<Any>().data(null).errors(listOf(toGraphQLError(mapOf("message"
                to "Fail")))).build()}
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
    |  authors: [User]
    |  @hydrated(
    |    service: "UserService"
    |    field: "usersByIds"
    |    arguments: [{name: "ids" value: "${'$'}source.authorIds"}]
    |    indexed: true
    |    batchSize: 2
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Issue {
    |  authorIds: [ID]
    |  id: ID
    |}
    |type Query {
    |  issues: [Issue]
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issues") { env ->
          listOf(Issues_Issue(authorIds = listOf("1"), id = "ISSUE-1"), Issues_Issue(authorIds =
              listOf("1", "2"), id = "ISSUE-2"), Issues_Issue(authorIds = listOf("2", "4"), id =
              "ISSUE-3"))}
      }
    }
    )
)) {
  private data class UserService_User(
    public val id: String? = null,
    public val name: String? = null,
  )

  private data class Issues_Issue(
    public val authorIds: List<String?>? = null,
    public val id: String? = null,
  )
}
