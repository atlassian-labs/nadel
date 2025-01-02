package graphql.nadel.tests.legacy.`extend type`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.Unit

public class `extending types from another service is possible with synthetic fields` :
    NadelLegacyIntegrationTest(query = """
|query {
|  root {
|    id
|    name
|    extension {
|      id
|      name
|    }
|  }
|  anotherRoot
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Service2",
    overallSchema="""
    |type Query {
    |  lookUpQuery: LookUpQuery
    |}
    |type LookUpQuery {
    |  lookup(id: ID): Extension
    |}
    |extend type Root {
    |  extension: Extension
    |  @hydrated(
    |    service: "Service2"
    |    field: "lookUpQuery.lookup"
    |    arguments: [{name: "id" value: "${'$'}source.id"}]
    |    identifiedBy: "id"
    |  )
    |}
    |type Extension {
    |  id: ID
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Extension {
    |  id: ID
    |  name: String
    |}
    |
    |type LookUpQuery {
    |  lookup(id: ID): Extension
    |}
    |
    |type Query {
    |  lookUpQuery: LookUpQuery
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("lookUpQuery") {
          Unit}
      }
      wiring.type("LookUpQuery") { type ->
        type.dataFetcher("lookup") { env ->
          if (env.getArgument<Any?>("id") == "rootId") {
            Service2_Extension(id = "rootId", name = "extensionName")}
          else {
            null}
        }
      }
    }
    )
, Service(name="Service1", overallSchema="""
    |extend type Query {
    |  root: Root
    |}
    |extend type Query {
    |  anotherRoot: String
    |}
    |type Root {
    |  id: ID
    |}
    |extend type Root {
    |  name: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  root: Root
    |}
    |
    |extend type Query {
    |  anotherRoot: String
    |}
    |
    |type Root {
    |  id: ID
    |}
    |
    |extend type Root {
    |  name: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("anotherRoot") { env ->
          "anotherRoot"}

        .dataFetcher("root") { env ->
          Service1_Root(id = "rootId", name = "rootName")}
      }
    }
    )
)) {
  private data class Service2_Extension(
    public val id: String? = null,
    public val name: String? = null,
  )

  private data class Service2_LookUpQuery(
    public val lookup: Service2_Extension? = null,
  )

  private data class Service1_Root(
    public val id: String? = null,
    public val name: String? = null,
  )
}
