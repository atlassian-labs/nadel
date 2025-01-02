package graphql.nadel.tests.legacy.`chained transforms`.`ari use case`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `ari argument transform on renamed field` : NadelLegacyIntegrationTest(query = """
|query {
|  issue(id: "ari:/i-forget-what-aris-actually-look-like/57") {
|    key
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="service",
    overallSchema="""
    |directive @interpretAri on ARGUMENT_DEFINITION
    |type Query {
    |  issue(id: ID @interpretAri): Issue @renamed(from: "issueById")
    |}
    |type Issue {
    |  key: ID @renamed(from: "id")
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  issueById(id: ID): Issue
    |}
    |type Issue {
    |  id: ID
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issueById") { env ->
          if (env.getArgument<Any?>("id") == "57") {
            Service_Issue(id = "57")}
          else {
            null}
        }
      }
    }
    )
)) {
  private data class Service_Issue(
    public val id: String? = null,
  )
}
