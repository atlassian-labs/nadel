package graphql.nadel

import graphql.nadel.hints.AllDocumentVariablesHint
import graphql.nadel.hints.InternalNamespaceTypenameResolutionHint
import graphql.nadel.hints.LegacyOperationNamesHint
import graphql.nadel.hints.NewResultMergerHint

data class NadelExecutionHints constructor(
    val legacyOperationNames: LegacyOperationNamesHint,
    val allDocumentVariablesHint: AllDocumentVariablesHint,
    val internalNamespaceTypenameResolution: InternalNamespaceTypenameResolutionHint,
    val newResultMerger: NewResultMergerHint,
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
        private var internalNamespaceTypenameResolution: InternalNamespaceTypenameResolutionHint =
            InternalNamespaceTypenameResolutionHint { false }
        private var newResultMerger: NewResultMergerHint = NewResultMergerHint { false }

        constructor()

        constructor(nadelExecutionHints: NadelExecutionHints) {
            legacyOperationNames = nadelExecutionHints.legacyOperationNames
            allDocumentVariablesHint = nadelExecutionHints.allDocumentVariablesHint
            internalNamespaceTypenameResolution = nadelExecutionHints.internalNamespaceTypenameResolution
            newResultMerger = nadelExecutionHints.newResultMerger
        }

        fun legacyOperationNames(flag: LegacyOperationNamesHint): Builder {
            legacyOperationNames = flag
            return this
        }

        fun allDocumentVariablesHint(flag: AllDocumentVariablesHint): Builder {
            allDocumentVariablesHint = flag
            return this
        }

        fun internalNamespaceTypenameResolution(flag: InternalNamespaceTypenameResolutionHint): Builder {
            internalNamespaceTypenameResolution = flag
            return this
        }

        fun newResultMerger(flag: NewResultMergerHint): Builder {
            newResultMerger = flag
            return this
        }

        fun build(): NadelExecutionHints {
            return NadelExecutionHints(
                legacyOperationNames,
                allDocumentVariablesHint,
                internalNamespaceTypenameResolution,
                newResultMerger,
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
