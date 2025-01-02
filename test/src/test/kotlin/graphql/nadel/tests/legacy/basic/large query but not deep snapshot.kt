// @formatter:off
package graphql.nadel.tests.legacy.basic

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`large query but not deep`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `large query but not deep snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            ExpectedServiceCall(
                service = "service",
                query = """
                | {
                |   bar: foo {
                |     child {
                |       child {
                |         child {
                |           child {
                |             child {
                |               child {
                |                 child {
                |                   child {
                |                     name
                |                   }
                |                 }
                |               }
                |             }
                |           }
                |         }
                |       }
                |       two: child {
                |         child {
                |           child {
                |             child {
                |               child {
                |                 child {
                |                   child {
                |                     name
                |                   }
                |                 }
                |               }
                |             }
                |           }
                |         }
                |       }
                |       three: child {
                |         child {
                |           child {
                |             child {
                |               child {
                |                 child {
                |                   child {
                |                     name
                |                   }
                |                 }
                |               }
                |               four: child {
                |                 child {
                |                   child {
                |                     name
                |                   }
                |                 }
                |               }
                |             }
                |           }
                |         }
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "bar": null
                |   }
                | }
                """.trimMargin(),
                delayedResults = listOfJsonStrings(
                ),
            ),
            ExpectedServiceCall(
                service = "service",
                query = """
                | {
                |   foo {
                |     child {
                |       child {
                |         child {
                |           child {
                |             child {
                |               child {
                |                 child {
                |                   child {
                |                     name
                |                   }
                |                 }
                |               }
                |             }
                |           }
                |         }
                |       }
                |     }
                |   }
                | }
                """.trimMargin(),
                variables = "{}",
                result = """
                | {
                |   "data": {
                |     "foo": null
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
     *     "foo": null,
     *     "bar": null
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "foo": null,
            |     "bar": null
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
