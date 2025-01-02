package graphql.nadel.tests.legacy.errors

import graphql.execution.DataFetcherResult
import graphql.nadel.engine.util.toGraphQLError
import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `errors and some data from a service execution are reflected in the result` :
    NadelLegacyIntegrationTest(query = """
|query {
|  hello {
|    name
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="MyService",
    overallSchema="""
    |type Query {
    |  hello: World
    |  helloWithArgs(arg1: String! arg2: String): World
    |}
    |type World {
    |  id: ID
    |  name: String
    |}
    |type Mutation {
    |  hello: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Mutation {
    |  hello: String
    |}
    |
    |type Query {
    |  hello: World
    |  helloWithArgs(arg1: String!, arg2: String): World
    |}
    |
    |type World {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("hello") { env ->
          DataFetcherResult.newResult<Any>().data(MyService_World(name =
              "World")).errors(listOf(toGraphQLError(mapOf("message" to "Problem1")),
              toGraphQLError(mapOf("message" to "Problem2")))).build()}
      }
    }
    )
)) {
  private data class MyService_World(
    public val id: String? = null,
    public val name: String? = null,
  )
}
