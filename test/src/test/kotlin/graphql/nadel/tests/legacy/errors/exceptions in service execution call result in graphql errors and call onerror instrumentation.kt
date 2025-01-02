package graphql.nadel.tests.legacy.errors

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class
    `exceptions in service execution call result in graphql errors and call onerror instrumentation`
    : NadelLegacyIntegrationTest(query = """
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
    }
    )
)) {
  private data class MyService_World(
    public val id: String? = null,
    public val name: String? = null,
  )
}
