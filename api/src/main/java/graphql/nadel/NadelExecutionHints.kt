package graphql.nadel

import graphql.nadel.hints.AllDocumentVariablesHint
import graphql.nadel.hints.LegacyOperationNamesHint
import graphql.nadel.hints.NewDocumentCompiler
import graphql.nadel.hints.RunCoerceTransform

data class NadelExecutionHints constructor(
    val legacyOperationNames: LegacyOperationNamesHint,
    val allDocumentVariablesHint: AllDocumentVariablesHint,
    val newDocumentCompiler: NewDocumentCompiler,
    val runCoerceTransform: RunCoerceTransform,
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
        private var newDocumentCompiler: NewDocumentCompiler = NewDocumentCompiler { false }
        private var runCoerceTransform: RunCoerceTransform = RunCoerceTransform { true }

        constructor()

        constructor(nadelExecutionHints: NadelExecutionHints) {
            legacyOperationNames = nadelExecutionHints.legacyOperationNames
            allDocumentVariablesHint = nadelExecutionHints.allDocumentVariablesHint
            newDocumentCompiler = nadelExecutionHints.newDocumentCompiler
            runCoerceTransform = nadelExecutionHints.runCoerceTransform
        }

        fun legacyOperationNames(flag: LegacyOperationNamesHint): Builder {
            legacyOperationNames = flag
            return this
        }

        fun allDocumentVariablesHint(flag: AllDocumentVariablesHint): Builder {
            allDocumentVariablesHint = flag
            return this
        }

        fun newDocumentCompiler(flag: NewDocumentCompiler): Builder {
            newDocumentCompiler = flag
            return this
        }

        fun runCoerceTransform(flag: RunCoerceTransform): Builder {
            runCoerceTransform = flag
            return this
        }

        fun build(): NadelExecutionHints {
            return NadelExecutionHints(
                legacyOperationNames,
                allDocumentVariablesHint,
                newDocumentCompiler,
                runCoerceTransform,
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
