package graphql.nadel.tests.legacy.instrumentation

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class
    `abort beginQueryExecution within instrumentation still calls enhancing instrumentation` :
    NadelLegacyIntegrationTest(query = """
|query OpName {
|  hello {
|    name
|  }
|  hello {
|    id
|  }
|}
|""".trimMargin(), variables = mapOf("var1" to "val1"), services = listOf(Service(name="MyService",
    overallSchema="""
    |type Query {
    |  hello: World
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
