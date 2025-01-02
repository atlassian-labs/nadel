package graphql.nadel.tests.legacy.renames.types

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String
import kotlin.collections.List

public class `renamed type in union` : NadelLegacyIntegrationTest(query = """
|query {
|  nodes {
|    __typename
|    ... on JiraIssue {
|      id
|      links {
|        __typename
|      }
|    }
|    ... on User {
|      id
|    }
|    ... on Donkey {
|      id
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="IssueService",
    overallSchema="""
    |type Query {
    |  nodes: [Node] @renamed(from: "all")
    |}
    |union Node = JiraIssue | User | Donkey
    |type JiraIssue @renamed(from: "Issue") {
    |  id: ID
    |  links: [Node]
    |}
    |type User {
    |  id: ID
    |}
    |type Donkey @renamed(from: "Monkey") {
    |  id: ID
    |}
    |""".trimMargin(), underlyingSchema="""
    |union Node = Issue | User | Monkey
    |type Query {
    |  all: [Node]
    |}
    |type Issue {
    |  id: ID
    |  links: [Node]
    |}
    |type User {
    |  id: ID
    |}
    |type Monkey {
    |  id: ID
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("all") { env ->
          listOf(IssueService_Issue(id = "1", links = null), null, IssueService_Issue(id = "2",
              links = listOf()), IssueService_Issue(id = "3", links = listOf(IssueService_User(),
              IssueService_Issue(), IssueService_Monkey())), IssueService_Monkey(id = "4"),
              IssueService_User(id = "8"))}
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
    public val id: String? = null,
    public val links: List<IssueService_Node?>? = null,
  ) : IssueService_Node

  private data class IssueService_Monkey(
    public val id: String? = null,
  ) : IssueService_Node

  private sealed interface IssueService_Node

  private data class IssueService_User(
    public val id: String? = null,
  ) : IssueService_Node
}
