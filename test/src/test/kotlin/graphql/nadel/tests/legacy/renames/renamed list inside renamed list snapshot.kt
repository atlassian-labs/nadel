// @formatter:off
package graphql.nadel.tests.legacy.renames

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`renamed list inside renamed list`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `renamed list inside renamed list snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "IssuesService",
                query = """
                | {
                |   rename__renamedIssue__issue: issue {
                |     __typename__rename__renamedTicket: __typename
                |     rename__renamedTicket__ticket: ticket {
                |       __typename__rename__renamedTicketTypes: __typename
                |       rename__renamedTicketTypes__ticketTypes: ticketTypes {
                |         __typename__rename__renamedId: __typename
                |         __typename__rename__renamedDate: __typename
                |         rename__renamedDate__date: date
                |         rename__renamedId__id: id
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "rename__renamedIssue__issue": [
                |       {
                |         "rename__renamedTicket__ticket": {
                |           "rename__renamedTicketTypes__ticketTypes": [
                |             {
                |               "rename__renamedId__id": "1",
                |               "__typename__rename__renamedId": "TicketType",
                |               "rename__renamedDate__date": "20/11/2020",
                |               "__typename__rename__renamedDate": "TicketType"
                |             }
                |           ],
                |           "__typename__rename__renamedTicketTypes": "Ticket"
                |         },
                |         "__typename__rename__renamedTicket": "Issue"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
        )

    /**
     * ```json
     * {
     *   "data": {
     *     "renamedIssue": [
     *       {
     *         "renamedTicket": {
     *           "renamedTicketTypes": [
     *             {
     *               "renamedId": "1",
     *               "renamedDate": "20/11/2020"
     *             }
     *           ]
     *         }
     *       }
     *     ]
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "renamedIssue": [
            |       {
            |         "renamedTicket": {
            |           "renamedTicketTypes": [
            |             {
            |               "renamedId": "1",
            |               "renamedDate": "20/11/2020"
            |             }
            |           ]
            |         }
            |       }
            |     ]
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
