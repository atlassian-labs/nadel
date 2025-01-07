package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest

class `renamed list inside renamed list` : NadelLegacyIntegrationTest(
    query = """
        query {
          renamedIssue {
            renamedTicket {
              renamedTicketTypes {
                renamedId
                renamedDate
              }
            }
          }
        }
    """.trimIndent(),
    variables = emptyMap(),
    services = listOf(
        Service(
            name = "IssuesService",
            overallSchema = """
                type Query {
                  renamedIssue: [RenamedIssue] @renamed(from: "issue")
                }
                type RenamedIssue @renamed(from: "Issue") {
                  renamedTicket: RenamedTicket @renamed(from: "ticket")
                }
                type RenamedTicket @renamed(from: "Ticket") {
                  renamedTicketTypes: [RenamedTicketType] @renamed(from: "ticketTypes")
                }
                type RenamedTicketType @renamed(from: "TicketType") {
                  renamedId: String @renamed(from: "id")
                  renamedDate: String @renamed(from: "date")
                }
            """.trimIndent(),
            underlyingSchema = """
                type Issue {
                  ticket: Ticket
                }
                type Query {
                  issue: [Issue]
                }
                type Ticket {
                  ticketTypes: [TicketType]
                }
                type TicketType {
                  date: String
                  id: String
                }
            """.trimIndent(),
            runtimeWiring = { wiring ->
                wiring.type("Query") { type ->
                    type.dataFetcher("issue") { env ->
                        listOf(
                            IssuesService_Issue(
                                ticket = IssuesService_Ticket(
                                    ticketTypes = listOf(IssuesService_TicketType(date = "20/11/2020", id = "1")),
                                ),
                            ),
                        )
                    }
                }
            },
        ),
    ),
) {
    private data class IssuesService_Issue(
        val ticket: IssuesService_Ticket? = null,
    )

    private data class IssuesService_Ticket(
        val ticketTypes: List<IssuesService_TicketType?>? = null,
    )

    private data class IssuesService_TicketType(
        val date: String? = null,
        val id: String? = null,
    )
}
