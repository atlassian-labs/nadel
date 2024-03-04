package graphql.nadel

import graphql.nadel.hints.AllDocumentVariablesHint
import graphql.nadel.hints.LegacyOperationNamesHint
import graphql.nadel.hints.NadelDeferSupportHint
import graphql.nadel.hints.NewBatchHydrationGroupingHint
import graphql.nadel.hints.NewResultMergerAndNamespacedTypename
import graphql.nadel.hints.ShortCircuitEmptyQueryHint

data class NadelExecutionHints(
    val legacyOperationNames: LegacyOperationNamesHint,
    val allDocumentVariablesHint: AllDocumentVariablesHint,
    val newResultMergerAndNamespacedTypename: NewResultMergerAndNamespacedTypename,
    val newBatchHydrationGrouping: NewBatchHydrationGroupingHint,
    val deferSupport: NadelDeferSupportHint,
    val shortCircuitEmptyQuery: ShortCircuitEmptyQueryHint,
) {
    /**
     * Returns a builder with the same field values as this object.
     *
     *
     * This is useful for transforming the object.
     */
    fun toBuilder(): Builder {
        return Builder(this)
    }

    class Builder {
        private var legacyOperationNames = LegacyOperationNamesHint { false }
        private var allDocumentVariablesHint = AllDocumentVariablesHint { false }
        private var newResultMergerAndNamespacedTypename = NewResultMergerAndNamespacedTypename { false }
        private var newBatchHydrationGrouping = NewBatchHydrationGroupingHint { false }
        private var deferSupport = NadelDeferSupportHint { false }
        private var shortCircuitEmptyQuery = ShortCircuitEmptyQueryHint { false }

        constructor()

        constructor(nadelExecutionHints: NadelExecutionHints) {
            legacyOperationNames = nadelExecutionHints.legacyOperationNames
            allDocumentVariablesHint = nadelExecutionHints.allDocumentVariablesHint
            newResultMergerAndNamespacedTypename = nadelExecutionHints.newResultMergerAndNamespacedTypename
            shortCircuitEmptyQuery = nadelExecutionHints.shortCircuitEmptyQuery
        }

        fun legacyOperationNames(flag: LegacyOperationNamesHint): Builder {
            legacyOperationNames = flag
            return this
        }

        fun allDocumentVariablesHint(flag: AllDocumentVariablesHint): Builder {
            allDocumentVariablesHint = flag
            return this
        }

        fun newResultMergerAndNamespacedTypename(flag: NewResultMergerAndNamespacedTypename): Builder {
            newResultMergerAndNamespacedTypename = flag
            return this
        }

        fun newBatchHydrationGrouping(flag: NewBatchHydrationGroupingHint): Builder {
            newBatchHydrationGrouping = flag
            return this
        }

        fun deferSupport(flag: NadelDeferSupportHint): Builder {
            deferSupport = flag
            return this
        }

        fun shortCircuitEmptyQuery(flag: ShortCircuitEmptyQueryHint): Builder {
            shortCircuitEmptyQuery = flag
            return this
        }

        fun build(): NadelExecutionHints {
            return NadelExecutionHints(
                legacyOperationNames,
                allDocumentVariablesHint,
                newResultMergerAndNamespacedTypename,
                newBatchHydrationGrouping,
                deferSupport,
                shortCircuitEmptyQuery,
            )
        }
    }

    companion object {
        @JvmStatic
        fun newHints(): Builder {
            return Builder()
        }
    }
}
