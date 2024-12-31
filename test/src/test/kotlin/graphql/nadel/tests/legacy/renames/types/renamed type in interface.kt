package graphql.nadel.tests.legacy.renames.types

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `renamed type in interface` : NadelLegacyIntegrationTest(
    query = """
        query {
          nodes {
            __typename
            id
            ... on JiraIssue {
              links {
                __typename
              }
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
                  nodes: [Node] @renamed(from: "all")
                }
                type JiraIssue implements Node @renamed(from: "Issue") {
                  id: ID
                  links: [Node]
                }
                interface Node {
                  id: ID
                }
                type User implements Node {
                  id: ID
                }
                type Donkey implements Node @renamed(from: "Monkey") {
                  id: ID
                }
            """.trimIndent(),
            underlyingSchema = """
                type Query {
                  all: [Node]
                }
                type Issue implements Node {
                  id: ID
                  links: [Node]
                }
                interface Node {
                  id: ID
                }
                type User implements Node {
                  id: ID
                }
                type Monkey implements Node {
                  id: ID
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("all") { env ->
                        listOf(
                            IssueService_Issue(id = "issue/1", links = null),
                            null,
                            IssueService_Issue(
                                id = "issue/2",
                                links = listOf(),
                            ),
                            IssueService_Issue(
                                id = "issue/3",
                                links =
                                listOf(
                                    IssueService_User(id = "user/1"),
                                    IssueService_Issue("issue/1"),
                                    IssueService_Monkey("monkey/1"),
                                ),
                            ),
                            IssueService_Monkey(id = "monkey/1"),
                            IssueService_User(id = "user/1"),
                        )
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
    ),
) {
    private data class IssueService_Issue(
        override val id: String? = null,
        val links: List<IssueService_Node?>? = null,
    ) : IssueService_Node

    private data class IssueService_Monkey(
        override val id: String? = null,
    ) : IssueService_Node

    private interface IssueService_Node {
        val id: String?
    }

    private data class IssueService_User(
        override val id: String? = null,
    ) : IssueService_Node
}
