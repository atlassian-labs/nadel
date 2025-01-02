package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `renamed top level field with argument` : NadelLegacyIntegrationTest(query = """
|query {
|  renameObject(id: "OBJECT-001") {
|    name
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="MyService",
    overallSchema="""
    |type Query {
    |  renameObject(id: ID!): ObjectOverall @renamed(from: "renameObjectUnderlying")
    |}
    |type ObjectOverall @renamed(from: "ObjectUnderlying") {
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type ObjectUnderlying {
    |  name: String
    |}
    |
    |type Query {
    |  renameObjectUnderlying(id: ID!): ObjectUnderlying
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("renameObjectUnderlying") { env ->
          if (env.getArgument<Any?>("id") == "OBJECT-001") {
            MyService_ObjectUnderlying(name = "Object 001")}
          else {
            null}
        }
      }
    }
    )
)) {
  private data class MyService_ObjectUnderlying(
    public val name: String? = null,
  )
}
