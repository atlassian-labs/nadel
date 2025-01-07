// @formatter:off
package graphql.nadel.tests.legacy.schema

import graphql.nadel.tests.next.ExpectedNadelResult
import graphql.nadel.tests.next.ExpectedServiceCall
import graphql.nadel.tests.next.TestSnapshot
import graphql.nadel.tests.next.listOfJsonStrings
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

private suspend fun main() {
    graphql.nadel.tests.next.update<`can delete fields and types`>()
}

/**
 * This class is generated. Do NOT modify.
 *
 * Refer to [graphql.nadel.tests.next.UpdateTestSnapshots
 */
@Suppress("unused")
public class `can delete fields and types snapshot` : TestSnapshot() {
    override val calls: List<ExpectedServiceCall> = listOf(
            )

    /**
     * ```json
     * {
     *   "data": {
     *     "__schema": {
     *       "types": [
     *         {
     *           "name": "Bar"
     *         },
     *         {
     *           "name": "Boolean"
     *         },
     *         {
     *           "name": "Float"
     *         },
     *         {
     *           "name": "Foo"
     *         },
     *         {
     *           "name": "ID"
     *         },
     *         {
     *           "name": "Int"
     *         },
     *         {
     *           "name": "JSON"
     *         },
     *         {
     *           "name": "NadelBatchObjectIdentifiedBy"
     *         },
     *         {
     *           "name": "NadelHydrationArgument"
     *         },
     *         {
     *           "name": "NadelHydrationCondition"
     *         },
     *         {
     *           "name": "NadelHydrationResultCondition"
     *         },
     *         {
     *           "name": "NadelHydrationResultFieldPredicate"
     *         },
     *         {
     *           "name": "Query"
     *         },
     *         {
     *           "name": "String"
     *         },
     *         {
     *           "name": "__Directive"
     *         },
     *         {
     *           "name": "__DirectiveLocation"
     *         },
     *         {
     *           "name": "__EnumValue"
     *         },
     *         {
     *           "name": "__Field"
     *         },
     *         {
     *           "name": "__InputValue"
     *         },
     *         {
     *           "name": "__Schema"
     *         },
     *         {
     *           "name": "__Type"
     *         },
     *         {
     *           "name": "__TypeKind"
     *         }
     *       ]
     *     }
     *   }
     * }
     * ```
     */
    override val result: ExpectedNadelResult = ExpectedNadelResult(
            result = """
            | {
            |   "data": {
            |     "__schema": {
            |       "types": [
            |         {
            |           "name": "Bar"
            |         },
            |         {
            |           "name": "Boolean"
            |         },
            |         {
            |           "name": "Float"
            |         },
            |         {
            |           "name": "Foo"
            |         },
            |         {
            |           "name": "ID"
            |         },
            |         {
            |           "name": "Int"
            |         },
            |         {
            |           "name": "JSON"
            |         },
            |         {
            |           "name": "NadelBatchObjectIdentifiedBy"
            |         },
            |         {
            |           "name": "NadelHydrationArgument"
            |         },
            |         {
            |           "name": "NadelHydrationCondition"
            |         },
            |         {
            |           "name": "NadelHydrationResultCondition"
            |         },
            |         {
            |           "name": "NadelHydrationResultFieldPredicate"
            |         },
            |         {
            |           "name": "Query"
            |         },
            |         {
            |           "name": "String"
            |         },
            |         {
            |           "name": "__Directive"
            |         },
            |         {
            |           "name": "__DirectiveLocation"
            |         },
            |         {
            |           "name": "__EnumValue"
            |         },
            |         {
            |           "name": "__Field"
            |         },
            |         {
            |           "name": "__InputValue"
            |         },
            |         {
            |           "name": "__Schema"
            |         },
            |         {
            |           "name": "__Type"
            |         },
            |         {
            |           "name": "__TypeKind"
            |         }
            |       ]
            |     }
            |   }
            | }
            """.trimMargin(),
            delayedResults = listOfJsonStrings(
            ),
        )
}
