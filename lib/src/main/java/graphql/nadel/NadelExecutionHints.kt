package graphql.nadel

import graphql.nadel.hints.AllDocumentVariablesHint
import graphql.nadel.hints.NadelDeferSupportHint

data class NadelExecutionHints(
    val allDocumentVariablesHint: AllDocumentVariablesHint,
    val deferSupport: NadelDeferSupportHint,
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
        private var allDocumentVariablesHint = AllDocumentVariablesHint { false }
        private var deferSupport = NadelDeferSupportHint { false }

        constructor()

        constructor(nadelExecutionHints: NadelExecutionHints) {
            allDocumentVariablesHint = nadelExecutionHints.allDocumentVariablesHint
            deferSupport = nadelExecutionHints.deferSupport
        }

        fun allDocumentVariablesHint(flag: AllDocumentVariablesHint): Builder {
            allDocumentVariablesHint = flag
            return this
        }

        fun deferSupport(flag: NadelDeferSupportHint): Builder {
            deferSupport = flag
            return this
        }

        fun build(): NadelExecutionHints {
            return NadelExecutionHints(
                allDocumentVariablesHint,
                deferSupport,
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
