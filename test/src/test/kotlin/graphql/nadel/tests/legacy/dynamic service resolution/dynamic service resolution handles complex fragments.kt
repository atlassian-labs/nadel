package graphql.nadel.tests.legacy.`dynamic service resolution`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.Any
import kotlin.String

public class `dynamic service resolution handles complex fragments` :
    NadelLegacyIntegrationTest(query = """
|{
|  node(id: "pull-request:id-123") {
|    ... {
|      ... {
|        ... on PullRequest {
|          id
|        }
|      }
|    }
|    ... on PullRequest {
|      description
|      author {
|        ... on User {
|          name
|          ... {
|            avatarUrl
|          }
|        }
|      }
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="shared",
    overallSchema="""
    |directive @dynamicServiceResolution on FIELD_DEFINITION
    |
    |type Query {
    |  node(id: ID!): Node @dynamicServiceResolution
    |}
    |
    |interface Node {
    |  id: ID!
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  echo: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
    }
    )
, Service(name="RepoService", overallSchema="""
    |type PullRequest implements Node {
    |  id: ID!
    |  description: String
    |  author: User
    |}
    |type User {
    |  name: String
    |  avatarUrl: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  node(id: ID): Node
    |}
    |
    |interface Node {
    |  id: ID!
    |}
    |
    |type PullRequest implements Node {
    |  id: ID!
    |  description: String
    |  author: User
    |}
    |
    |type User {
    |  name: String
    |  avatarUrl: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("node") { env ->
          if (env.getArgument<Any?>("id") == "pull-request:id-123") {
            RepoService_PullRequest(id = "pull-request:id-123", author = RepoService_User(avatarUrl
                = "https://avatar.acme.com/user-123", name = "I'm an User"), description =
                "this is a pull request")}
          else {
            null}
        }
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
, Service(name="IssueService", overallSchema="""
    |type Issue implements Node {
    |  id: ID!
    |  issueKey: String
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Query {
    |  node(id: ID): Node
    |}
    |
    |interface Node {
    |  id: ID!
    |}
    |
    |type Issue implements Node {
    |  id: ID!
    |  issueKey: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
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
  private interface RepoService_Node {
    public val id: String?
  }

  private data class RepoService_PullRequest(
    override val id: String? = null,
    public val description: String? = null,
    public val author: RepoService_User? = null,
  ) : RepoService_Node

  private data class RepoService_User(
    public val name: String? = null,
    public val avatarUrl: String? = null,
  )

  private data class IssueService_Issue(
    override val id: String? = null,
    public val issueKey: String? = null,
  ) : IssueService_Node

  private interface IssueService_Node {
    public val id: String?
  }
}
