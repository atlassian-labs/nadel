package graphql.nadel.tests.legacy.`extend type`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

public class `extending types via hydration returning a connection` :
    NadelLegacyIntegrationTest(query = """
|query {
|  synth {
|    issue {
|      association(filter: {name: "value"}) {
|        nodes {
|          page {
|            id
|          }
|        }
|      }
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="Issue", overallSchema="""
    |type Query {
    |  synth: Synth
    |}
    |type Synth {
    |  issue: Issue
    |}
    |type Issue {
    |  id: ID
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Issue {
    |  id: ID
    |}
    |
    |type Query {
    |  synth: Synth
    |}
    |
    |type Synth {
    |  issue: Issue
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("synth") { env ->
          Issue_Synth(issue = Issue_Issue(id = "ISSUE-1"))}
      }
    }
    )
, Service(name="Association", overallSchema="""
    |type Query {
    |  association(id: ID, filter: Filter): AssociationConnection
    |  pages: Pages
    |}
    |type Pages {
    |  page(id: ID): Page
    |}
    |type AssociationConnection {
    |  nodes: [Association]
    |}
    |input Filter {
    |  name: String
    |}
    |type Association {
    |  id: ID
    |  nameOfAssociation: String
    |  page: Page
    |  @hydrated(
    |    service: "Association"
    |    field: "pages.page"
    |    arguments: [{name: "id" value: "${'$'}source.pageId"}]
    |  )
    |}
    |type Page {
    |  id: ID
    |}
    |extend type Issue {
    |  association(filter: Filter): AssociationConnection
    |  @hydrated(
    |    service: "Association"
    |    field: "association"
    |    arguments: [{name: "id" value: "${'$'}source.id"} {name: "filter" value: "${'$'}argument.filter"}]
    |  )
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Association {
    |  id: ID
    |  nameOfAssociation: String
    |  pageId: ID
    |}
    |
    |type AssociationConnection {
    |  nodes: [Association]
    |}
    |
    |type Page {
    |  id: ID
    |}
    |
    |type Pages {
    |  page(id: ID): Page
    |}
    |
    |type Query {
    |  association(filter: Filter, id: ID): AssociationConnection
    |  pages: Pages
    |}
    |
    |input Filter {
    |  name: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("pages") {
          Unit}
      }
      wiring.type("Query") { type ->
        type.dataFetcher("association") { env ->
          if (env.getArgument<Any?>("filter") == mapOf("name" to "value") &&
              env.getArgument<Any?>("id") == "ISSUE-1") {
            Association_AssociationConnection(nodes = listOf(Association_Association(pageId =
                "1")))}
          else {
            null}
        }
      }

      wiring.type("Pages") { type ->
        type.dataFetcher("page") { env ->
          if (env.getArgument<Any?>("id") == "1") {
            Association_Page(id = "1")}
          else {
            null}
        }
      }
    }
    )
)) {
  private data class Issue_Issue(
    public val id: String? = null,
  )

  private data class Issue_Synth(
    public val issue: Issue_Issue? = null,
  )

  private data class Association_Association(
    public val id: String? = null,
    public val nameOfAssociation: String? = null,
    public val pageId: String? = null,
  )

  private data class Association_AssociationConnection(
    public val nodes: List<Association_Association?>? = null,
  )

  private data class Association_Filter(
    public val name: String? = null,
  )

  private data class Association_Page(
    public val id: String? = null,
  )

  private data class Association_Pages(
    public val page: Association_Page? = null,
  )
}
