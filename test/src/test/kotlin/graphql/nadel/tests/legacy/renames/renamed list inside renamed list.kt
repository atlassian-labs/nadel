package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.legacy.NadelLegacyIntegrationTest
import kotlin.String
import kotlin.collections.List

public class `renamed list inside renamed list` : NadelLegacyIntegrationTest(query = """
|query {
|  renamedIssue {
|    renamedTicket {
|      renamedTicketTypes {
|        renamedId
|        renamedDate
|      }
|    }
|  }
|}
|""".trimMargin(), variables = emptyMap(), services = listOf(Service(name="IssuesService",
    overallSchema="""
    |type Query {
    |  renamedIssue: [RenamedIssue] @renamed(from: "issue")
    |}
    |type RenamedIssue @renamed(from: "Issue") {
    |  renamedTicket: RenamedTicket @renamed(from: "ticket")
    |}
    |type RenamedTicket @renamed(from: "Ticket") {
    |  renamedTicketTypes: [RenamedTicketType] @renamed(from: "ticketTypes")
    |}
    |type RenamedTicketType @renamed(from: "TicketType") {
    |  renamedId: String @renamed(from: "id")
    |  renamedDate: String @renamed(from: "date")
    |}
    |""".trimMargin(), underlyingSchema="""
    |type Issue {
    |  ticket: Ticket
    |}
    |
    |type Query {
    |  issue: [Issue]
    |}
    |
    |type Ticket {
    |  ticketTypes: [TicketType]
    |}
    |
    |type TicketType {
    |  date: String
    |  id: String
    |}
    |""".trimMargin(), runtimeWiring = { wiring ->
      wiring.type("Query") { type ->
        type.dataFetcher("issue") { env ->
          listOf(IssuesService_Issue(ticket = IssuesService_Ticket(ticketTypes =
              listOf(IssuesService_TicketType(date = "20/11/2020", id = "1")))))}
      }
    }
    )
)) {
  private data class IssuesService_Issue(
    public val ticket: IssuesService_Ticket? = null,
  )

  private data class IssuesService_Ticket(
    public val ticketTypes: List<IssuesService_TicketType?>? = null,
  )

  private data class IssuesService_TicketType(
    public val date: String? = null,
    public val id: String? = null,
  )
}
