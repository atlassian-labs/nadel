package graphql.nadel.tests.legacy.renames.types

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `renamed type in union declared in another service` : NadelLegacyIntegrationTest(
    query = """
        query {
          nodes {
            __typename
            ... on Issue {
              id
            }
            ... on JiraComment {
              id
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "IssueService",
            overallSchema = """
                type Query {
                  nodes: [Node]
                }
                type Issue {
                  id: ID
                }
                union Node = Issue | JiraComment
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  nodes: [Node]
                }
                type Issue {
                  id: ID
                }
                type Comment {
                  id: ID
                }
                union Node = Issue | Comment
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("nodes") { env ->
                        listOf(IssueService_Issue(id = "1"), IssueService_Comment(id = "2"))
                    }
                }
                wiring.type("Node") { type ->
                    type.typeResolver { typeResolver ->
                        val obj = typeResolver.getObject<Any>()
                        val typeName = obj.javaClass.simpleName.substringAfter("_")
                        typeResolver.schema.getTypeAs(typeName)
                    }
                }
            },
        ),
        Service(
            name = "CommentService",
            overallSchema = """
                type Query {
                  comment: JiraComment
                }
                type JiraComment @renamed(from: "Comment") {
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  comment: Comment
                }
                type Comment {
                  id: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
            },
        ),
    ),
) {
    private data class IssueService_Comment(
        val id: String? = null,
    ) : IssueService_Node

    private data class IssueService_Issue(
        val id: String? = null,
    ) : IssueService_Node

    private sealed interface IssueService_Node

    private data class CommentService_Comment(
        val id: String? = null,
    )
}
