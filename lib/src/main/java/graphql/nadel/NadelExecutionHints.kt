package graphql.nadel

import graphql.nadel.hints.AllDocumentVariablesHint
import graphql.nadel.hints.LegacyOperationNamesHint

data class NadelExecutionHints constructor(
    val legacyOperationNames: LegacyOperationNamesHint,
    val allDocumentVariablesHint: AllDocumentVariablesHint,
    val removeHydrationSpecificExecutionCode: Boolean,
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
        private var removeHydrationSpecificExecutionCode: Boolean = false

        constructor()

        constructor(nadelExecutionHints: NadelExecutionHints) {
            legacyOperationNames = nadelExecutionHints.legacyOperationNames
            allDocumentVariablesHint = nadelExecutionHints.allDocumentVariablesHint
            removeHydrationSpecificExecutionCode = nadelExecutionHints.removeHydrationSpecificExecutionCode
        }

        fun legacyOperationNames(flag: LegacyOperationNamesHint): Builder {
            legacyOperationNames = flag
            return this
        }

        fun allDocumentVariablesHint(flag: AllDocumentVariablesHint): Builder {
            allDocumentVariablesHint = flag
            return this
        }

        fun removeHydrationSpecificExecutionCode(flag: Boolean): Builder {
            removeHydrationSpecificExecutionCode = flag
            return this
        }

        fun build(): NadelExecutionHints {
            return NadelExecutionHints(
                legacyOperationNames,
                allDocumentVariablesHint,
                removeHydrationSpecificExecutionCode,
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
