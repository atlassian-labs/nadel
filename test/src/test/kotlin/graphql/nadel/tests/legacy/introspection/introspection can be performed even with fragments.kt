package graphql.nadel.tests.legacy.introspection

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String

public class `introspection can be performed even with fragments` : NadelLegacyIntegrationTest(query
    = """
|query {
|  ...IntrospectionFrag
|  __type(name: "World") {
|    name
|  }
|  __typename
|}
|
|fragment IntrospectionFrag on Query {
|  __schema {
|    queryType {
|      name
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="MyService",
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
