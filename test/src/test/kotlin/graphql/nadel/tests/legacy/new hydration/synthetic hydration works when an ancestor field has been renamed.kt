package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

public class `synthetic hydration works when an ancestor field has been renamed` :
    NadelLegacyIntegrationTest(query = """
|query {
|  devOpsRelationships {
|    devOpsNodes {
|      devOpsIssue {
|        id
|      }
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="IssueService",
    overallSchema="""
    |type DevOpsIssue @renamed(from: "Issue") {
    |  id: ID
    |}
    |type DevOpsRelationship @renamed(from: "Relationship") {
    |  devOpsIssue: DevOpsIssue
    |  @hydrated(
    |    service: "IssueService"
    |    field: "syntheticIssue.issue"
    |    arguments: [{name: "id" value: "${'$'}source.issueId"}]
    |  )
    |}
    |type DevOpsRelationshipConnection @renamed(from: "RelationshipConnection") {
    |  devOpsNodes: [DevOpsRelationship] @renamed(from: "nodes")
    |}
    |type SyntheticIssue {
    |  devOpsIssue(id: ID): DevOpsIssue @renamed(from: "issue")
    |  issue(id: ID): DevOpsIssue
    |}
    |type Query {
    |  devOpsRelationships: DevOpsRelationshipConnection @renamed(from: "relationships")
    |  syntheticIssue: SyntheticIssue
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Issue {
    |  id: ID
    |}
    |
    |type Query {
    |  relationships: RelationshipConnection
    |  syntheticIssue: SyntheticIssue
    |}
    |
    |type Relationship {
    |  issueId: ID
    |}
    |
    |type RelationshipConnection {
    |  nodes: [Relationship]
    |}
    |
    |type SyntheticIssue {
    |  issue(id: ID): Issue
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("syntheticIssue") {
          Unit}
      }
      wiring.type("Query") { type ->
        type.dataFetcher("relationships") { env ->
          IssueService_RelationshipConnection(nodes = listOf(IssueService_Relationship(issueId =
              "1")))}
      }

      wiring.type("SyntheticIssue") { type ->
        type.dataFetcher("issue") { env ->
          if (env.getArgument<Any?>("id") == "1") {
            IssueService_Issue(id = "1")}
          else {
            null}
        }
      }
    }
    )
)) {
  private data class IssueService_Issue(
    public val id: String? = null,
  )

  private data class IssueService_Relationship(
    public val issueId: String? = null,
  )

  private data class IssueService_RelationshipConnection(
    public val nodes: List<IssueService_Relationship?>? = null,
  )

  private data class IssueService_SyntheticIssue(
    public val issue: IssueService_Issue? = null,
  )
}
