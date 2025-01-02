package graphql.nadel.tests.legacy.`result merging`

import graphql.execution.DataFetcherResult
import graphql.nadel.engine.util.toGraphQLError
import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any

public class `correct selection set on partially failed result` : NadelLegacyIntegrationTest(query =
    """
|query {
|  foo
|  bar
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="foo", overallSchema="""
    |type Query {
    |  foo: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  foo: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("foo") { env ->
          DataFetcherResult.newResult<Any>().data(null).errors(listOf(toGraphQLError(mapOf("message"
              to "Test")))).build()}
      }
    }
    )
, Service(name="bar", overallSchema="""
    |type Query {
    |  bar: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  bar: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("bar") { env ->
          "Hello"}
      }
    }
    )
))
