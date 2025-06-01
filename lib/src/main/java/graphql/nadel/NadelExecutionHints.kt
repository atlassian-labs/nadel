package graphql.nadel

import graphql.nadel.hints.AllDocumentVariablesHint
import graphql.nadel.hints.LegacyOperationNamesHint
import graphql.nadel.hints.NadelDeferSupportHint
import graphql.nadel.hints.NadelShortCircuitEmptyQueryHint
import graphql.nadel.hints.NadelVirtualTypeSupportHint
import graphql.nadel.hints.NewResultMergerAndNamespacedTypename

data class NadelExecutionHints(
    val legacyOperationNames: LegacyOperationNamesHint,
    val allDocumentVariablesHint: AllDocumentVariablesHint,
    val newResultMergerAndNamespacedTypename: NewResultMergerAndNamespacedTypename,
    val deferSupport: NadelDeferSupportHint,
    val shortCircuitEmptyQuery: NadelShortCircuitEmptyQueryHint,
    val virtualTypeSupport: NadelVirtualTypeSupportHint,
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
        private var deferSupport = NadelDeferSupportHint { false }
        private var shortCircuitEmptyQuery = NadelShortCircuitEmptyQueryHint { false }
        private var virtualTypeSupport = NadelVirtualTypeSupportHint { false }

        constructor()

        constructor(nadelExecutionHints: NadelExecutionHints) {
            legacyOperationNames = nadelExecutionHints.legacyOperationNames
            allDocumentVariablesHint = nadelExecutionHints.allDocumentVariablesHint
            newResultMergerAndNamespacedTypename = nadelExecutionHints.newResultMergerAndNamespacedTypename
            deferSupport = nadelExecutionHints.deferSupport
            shortCircuitEmptyQuery = nadelExecutionHints.shortCircuitEmptyQuery
            virtualTypeSupport = nadelExecutionHints.virtualTypeSupport
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

        fun deferSupport(flag: NadelDeferSupportHint): Builder {
            deferSupport = flag
            return this
        }

        fun shortCircuitEmptyQuery(flag: NadelShortCircuitEmptyQueryHint): Builder {
            shortCircuitEmptyQuery = flag
            return this
        }

        fun virtualTypeSupport(flag: NadelVirtualTypeSupportHint): Builder {
            virtualTypeSupport = flag
            return this
        }

        fun build(): NadelExecutionHints {
            return NadelExecutionHints(
                legacyOperationNames,
                allDocumentVariablesHint,
                newResultMergerAndNamespacedTypename,
                deferSupport,
                shortCircuitEmptyQuery,
                virtualTypeSupport,
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
