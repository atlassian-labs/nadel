package graphql.nadel.tests.legacy.renames.types

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String
import kotlin.collections.List

public class `renamed type in interface` : NadelLegacyIntegrationTest(query = """
|query {
|  nodes {
|    __typename
|    id
|    ... on JiraIssue {
|      links {
|        __typename
|      }
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="IssueService",
    overallSchema="""
    |type Query {
    |  nodes: [Node] @renamed(from: "all")
    |}
    |type JiraIssue implements Node @renamed(from: "Issue") {
    |  id: ID
    |  links: [Node]
    |}
    |interface Node {
    |  id: ID
    |}
    |type User implements Node {
    |  id: ID
    |}
    |type Donkey implements Node @renamed(from: "Monkey") {
    |  id: ID
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  all: [Node]
    |}
    |type Issue implements Node {
    |  id: ID
    |  links: [Node]
    |}
    |interface Node {
    |  id: ID
    |}
    |type User implements Node {
    |  id: ID
    |}
    |type Monkey implements Node {
    |  id: ID
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("all") { env ->
          listOf(IssueService_Issue(id = null, links = null), null, IssueService_Issue(id = null,
              links = listOf()), IssueService_Issue(id = null, links = listOf(IssueService_User(),
              IssueService_Issue(), IssueService_Monkey())), IssueService_Monkey(id = null),
              IssueService_User(id = null))}
      }
      wiring.type("Node") { type ->
        type.typeResolver { typeResolver ->
          val obj = typeResolver.getObject<Any>()
          val typeName = obj.javaClass.simpleName.substringAfter("_")
          typeResolver.schema.getTypeAs(typeName)
        }
      }
    }
    )
)) {
  private data class IssueService_Issue(
    override val id: String? = null,
    public val links: List<IssueService_Node?>? = null,
  ) : IssueService_Node

  private data class IssueService_Monkey(
    override val id: String? = null,
  ) : IssueService_Node

  private interface IssueService_Node {
    public val id: String?
  }

  private data class IssueService_User(
    override val id: String? = null,
  ) : IssueService_Node
}
