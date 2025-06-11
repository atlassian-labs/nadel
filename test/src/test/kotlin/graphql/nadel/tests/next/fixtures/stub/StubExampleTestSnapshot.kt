// @formatter:off
package graphql.nadel.tests.next.fixtures.stub

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<StubExampleTest>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots]
 */
@Suppress("unused")
public class StubExampleTestSnapshot : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "myService",
                query = """
                | {
                |   myExistingType {
                |     __typename__stubbed__newFieldWithExistingType: __typename
                |     __typename__stubbed__newFieldWithComplexType: __typename
                |     existingField
                |     otherExistingField {
                |       __typename__stubbed__field2: __typename
                |       field1
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "myExistingType": {
                |       "existingField": "prod-data",
                |       "otherExistingField": {
                |         "field1": "otherProdData",
                |         "__typename__stubbed__field2": "OtherExistingType"
                |       },
                |       "__typename__stubbed__newFieldWithExistingType": "MyExistingType",
                |       "__typename__stubbed__newFieldWithComplexType": "MyExistingType"
                |     }
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
     *     "myExistingType": {
     *       "existingField": "prod-data",
     *       "otherExistingField": {
     *         "field1": "otherProdData",
     *         "field2": null
     *       },
     *       "newFieldWithComplexType": null,
     *       "newFieldWithExistingType": null
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "myExistingType": {
            |       "existingField": "prod-data",
            |       "otherExistingField": {
            |         "field1": "otherProdData",
            |         "field2": null
            |       },
            |       "newFieldWithComplexType": null,
            |       "newFieldWithExistingType": null
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
