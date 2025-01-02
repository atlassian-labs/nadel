// @formatter:off
package graphql.nadel.tests.legacy.`new hydration`

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`query with three nested hydrations and simple data and lots of renames`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `query with three nested hydrations and simple data and lots of renames snapshot` :
        TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "Bar",
                query = """
                | {
                |   barsById(id: ["bar1"]) {
                |     __typename__rename__barName: __typename
                |     __typename__batch_hydration__nestedBar: __typename
                |     batch_hydration__bar__barId: barId
                |     rename__barName__name: name
                |     batch_hydration__nestedBar__nestedBarId: nestedBarId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "barsById": [
                |       {
                |         "rename__barName__name": "Bar 1",
                |         "__typename__rename__barName": "Bar",
                |         "batch_hydration__nestedBar__nestedBarId": "nestedBar1",
                |         "__typename__batch_hydration__nestedBar": "Bar",
                |         "batch_hydration__bar__barId": "bar1"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Bar",
                query = """
                | {
                |   barsById(id: ["nestedBar1"]) {
                |     __typename__rename__barName: __typename
                |     __typename__batch_hydration__nestedBar: __typename
                |     batch_hydration__nestedBar__barId: barId
                |     rename__barName__name: name
                |     batch_hydration__nestedBar__nestedBarId: nestedBarId
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "barsById": [
                |       {
                |         "rename__barName__name": "NestedBarName1",
                |         "__typename__rename__barName": "Bar",
                |         "batch_hydration__nestedBar__nestedBarId": "nestedBarId456",
                |         "__typename__batch_hydration__nestedBar": "Bar",
                |         "batch_hydration__nestedBar__barId": "nestedBar1"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Bar",
                query = """
                | {
                |   barsById(id: ["nestedBarId456"]) {
                |     __typename__rename__barName: __typename
                |     __typename__rename__barDetails: __typename
                |     batch_hydration__nestedBar__barId: barId
                |     rename__barDetails__details: details {
                |       __typename__rename__barAge: __typename
                |       __typename__rename__barContact: __typename
                |       rename__barAge__age: age
                |       rename__barContact__contact: contact {
                |         __typename__rename__barEmail: __typename
                |         __typename__rename__barPhone: __typename
                |         rename__barEmail__email: email
                |         rename__barPhone__phone: phone
                |       }
                |     }
                |     rename__barName__name: name
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "barsById": [
                |       {
                |         "rename__barName__name": "NestedBarName2",
                |         "__typename__rename__barName": "Bar",
                |         "rename__barDetails__details": {
                |           "rename__barAge__age": 1,
                |           "__typename__rename__barAge": "Details",
                |           "rename__barContact__contact": {
                |             "rename__barEmail__email": "test",
                |             "__typename__rename__barEmail": "ContactDetails",
                |             "rename__barPhone__phone": 1,
                |             "__typename__rename__barPhone": "ContactDetails"
                |           },
                |           "__typename__rename__barContact": "Details"
                |         },
                |         "__typename__rename__barDetails": "Bar",
                |         "batch_hydration__nestedBar__barId": "nestedBarId456"
                |       }
                |     ]
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "Foo",
                query = """
                | {
                |   rename__fooz__foos: foos {
                |     __typename__rename__fooDetails: __typename
                |     __typename__batch_hydration__bar: __typename
                |     batch_hydration__bar__barId: barId
                |     rename__fooDetails__details: details {
                |       __typename__rename__fooName: __typename
                |       __typename__rename__fooAge: __typename
                |       __typename__rename__fooContact: __typename
                |       rename__fooAge__age: age
                |       rename__fooContact__contact: contact {
                |         __typename__rename__fooEmail: __typename
                |         __typename__rename__fooPhone: __typename
                |         rename__fooEmail__email: email
                |         rename__fooPhone__phone: phone
                |       }
                |       rename__fooName__name: name
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "rename__fooz__foos": [
                |       {
                |         "rename__fooDetails__details": {
                |           "rename__fooName__name": "smith",
                |           "__typename__rename__fooName": "Details",
                |           "rename__fooAge__age": 1,
                |           "__typename__rename__fooAge": "Details",
                |           "rename__fooContact__contact": {
                |             "rename__fooEmail__email": "test",
                |             "__typename__rename__fooEmail": "ContactDetails",
                |             "rename__fooPhone__phone": 1,
                |             "__typename__rename__fooPhone": "ContactDetails"
                |           },
                |           "__typename__rename__fooContact": "Details"
                |         },
                |         "__typename__rename__fooDetails": "Foo",
                |         "batch_hydration__bar__barId": "bar1",
                |         "__typename__batch_hydration__bar": "Foo"
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
     *     "fooz": [
     *       {
     *         "fooDetails": {
     *           "fooName": "smith",
     *           "fooAge": 1,
     *           "fooContact": {
     *             "fooEmail": "test",
     *             "fooPhone": 1
     *           }
     *         },
     *         "bar": {
     *           "barName": "Bar 1",
     *           "nestedBar": {
     *             "barName": "NestedBarName1",
     *             "nestedBar": {
     *               "barName": "NestedBarName2",
     *               "barDetails": {
     *                 "barAge": 1,
     *                 "barContact": {
     *                   "barEmail": "test",
     *                   "barPhone": 1
     *                 }
     *               }
     *             }
     *           }
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
            |     "fooz": [
            |       {
            |         "fooDetails": {
            |           "fooName": "smith",
            |           "fooAge": 1,
            |           "fooContact": {
            |             "fooEmail": "test",
            |             "fooPhone": 1
            |           }
            |         },
            |         "bar": {
            |           "barName": "Bar 1",
            |           "nestedBar": {
            |             "barName": "NestedBarName1",
            |             "nestedBar": {
            |               "barName": "NestedBarName2",
            |               "barDetails": {
            |                 "barAge": 1,
            |                 "barContact": {
            |                   "barEmail": "test",
            |                   "barPhone": 1
            |                 }
            |               }
            |             }
            |           }
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
