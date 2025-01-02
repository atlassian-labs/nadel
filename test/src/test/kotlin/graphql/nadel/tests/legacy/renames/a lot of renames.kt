package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `a lot of renames` : NadelLegacyIntegrationTest(
    query = """
        query {
          boardScope {
            cardParents {
              cardType {
                id
                inlineCardCreate {
                  enabled
                }
              }
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "Boards",
            overallSchema = """
                type Query {
                  boardScope: BoardScope
                }
                type BoardScope {
                  cardParents: [CardParent]! @renamed(from: "issueParents")
                }
                type CardParent @renamed(from: "IssueParent") {
                  cardType: CardType! @renamed(from: "issueType")
                }
                type CardType @renamed(from: "IssueType") {
                  id: ID
                  inlineCardCreate: InlineCardCreateConfig @renamed(from: "inlineIssueCreate")
                }
                type InlineCardCreateConfig @renamed(from: "InlineIssueCreateConfig") {
                  enabled: Boolean!
                }
            """.trimIndent(),
            underlyingSchema = """
                type BoardScope {
                  issueParents: [IssueParent]!
                }
                type InlineIssueCreateConfig {
                  enabled: Boolean!
                }
                type IssueParent {
                  issueType: IssueType!
                }
                type IssueType {
                  id: ID
                  inlineIssueCreate: InlineIssueCreateConfig
                }
                type Query {
                  boardScope: BoardScope
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("boardScope") { env ->
                        Boards_BoardScope(
                            issueParents = listOf(
                                Boards_IssueParent(
                                    issueType = Boards_IssueType(
                                        id = "ID-1",
                                        inlineIssueCreate = Boards_InlineIssueCreateConfig(enabled = true),
                                    ),
                                ),
                            ),
                        )
                    }
                }
            },
        ),
    ),
) {
    private data class Boards_BoardScope(
        val issueParents: List<Boards_IssueParent?>? = null,
    )

    private data class Boards_InlineIssueCreateConfig(
        val enabled: Boolean? = null,
    )

    private data class Boards_IssueParent(
        val issueType: Boards_IssueType? = null,
    )

    private data class Boards_IssueType(
        val id: String? = null,
        val inlineIssueCreate: Boards_InlineIssueCreateConfig? = null,
    )
}
