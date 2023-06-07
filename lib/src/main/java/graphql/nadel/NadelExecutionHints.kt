package graphql.nadel

import graphql.nadel.hints.LegacyOperationNamesHint

data class NadelExecutionHints(
    val legacyOperationNames: LegacyOperationNamesHint,
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

        constructor()

        constructor(nadelExecutionHints: NadelExecutionHints) {
            legacyOperationNames = nadelExecutionHints.legacyOperationNames
        }

        fun legacyOperationNames(flag: LegacyOperationNamesHint): Builder {
            legacyOperationNames = flag
            return this
        }

        fun build(): NadelExecutionHints {
            return NadelExecutionHints(
                legacyOperationNames,
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
