package graphql.nadel

import graphql.nadel.hints.AllDocumentVariablesHint
import graphql.nadel.hints.LegacyOperationNamesHint
import graphql.nadel.hints.NewResultMergerAndNamespacedTypename

data class NadelExecutionHints constructor(
    val legacyOperationNames: LegacyOperationNamesHint,
    val allDocumentVariablesHint: AllDocumentVariablesHint,
    val newResultMergerAndNamespacedTypename: NewResultMergerAndNamespacedTypename,
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
        private var legacyOperationNames: LegacyOperationNamesHint = LegacyOperationNamesHint { false }
        private var allDocumentVariablesHint: AllDocumentVariablesHint = AllDocumentVariablesHint { false }
        private var newResultMergerAndNamespacedTypename: NewResultMergerAndNamespacedTypename = NewResultMergerAndNamespacedTypename { false }

        constructor()

        constructor(nadelExecutionHints: NadelExecutionHints) {
            legacyOperationNames = nadelExecutionHints.legacyOperationNames
            allDocumentVariablesHint = nadelExecutionHints.allDocumentVariablesHint
            newResultMergerAndNamespacedTypename = nadelExecutionHints.newResultMergerAndNamespacedTypename
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

        fun build(): NadelExecutionHints {
            return NadelExecutionHints(
                legacyOperationNames,
                allDocumentVariablesHint,
                newResultMergerAndNamespacedTypename,
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
