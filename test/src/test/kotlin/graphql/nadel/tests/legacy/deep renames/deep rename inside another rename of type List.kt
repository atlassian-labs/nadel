package graphql.nadel.tests.legacy.`deep renames`

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `deep rename inside another rename of type List` : NadelLegacyIntegrationTest(
    query = """
        query {
          boardScope {
            board {
              cardChildren {
                id
                key
                summary
              }
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "Issues",
            overallSchema = """
                type Query {
                  boardScope: BoardScope
                }
                type BoardScope {
                  board: SoftwareBoard
                }
                type SoftwareBoard @renamed(from: "Board") {
                  cardChildren: [SoftwareCard] @renamed(from: "issueChildren")
                }
                type SoftwareCard @renamed(from: "Card") {
                  id: ID
                  key: String @renamed(from: "issue.key")
                  summary: String @renamed(from: "issue.summary")
                }
            """.trimIndent(),
            underlyingSchema = """
                type Board {
                  id: ID
                  issueChildren: [Card]
                }
                type BoardScope {
                  board: Board
                }
                type Card {
                  id: ID
                  issue: Issue
                }
                type Issue {
                  id: ID
                  key: String
                  summary: String
                }
                type Query {
                  boardScope: BoardScope
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("boardScope") { env ->
                        Issues_BoardScope(
                            board =
                            Issues_Board(
                                issueChildren =
                                listOf(
                                    Issues_Card(
                                        id = "1234",
                                        issue = Issues_Issue(key = "abc", summary = "Summary 1"),
                                    ),
                                    Issues_Card(
                                        id = "456",
                                        issue = Issues_Issue(key = "def", summary = "Summary 2"),
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
    private data class Issues_Board(
        val id: String? = null,
        val issueChildren: List<Issues_Card?>? = null,
    )

    private data class Issues_BoardScope(
        val board: Issues_Board? = null,
    )

    private data class Issues_Card(
        val id: String? = null,
        val issue: Issues_Issue? = null,
    )

    private data class Issues_Issue(
        val id: String? = null,
        val key: String? = null,
        val summary: String? = null,
    )
}
