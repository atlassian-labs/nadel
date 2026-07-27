package graphql.nadel

import graphql.nadel.hints.LegacyOperationNamesHint
import graphql.nadel.hints.NadelBatchRootFieldsHint
import graphql.nadel.hints.NadelDeferSupportHint
import graphql.nadel.hints.NadelDisableSharedTypesHint
import graphql.nadel.hints.NadelExecuteOnEngineSchemaHint
import graphql.nadel.hints.NadelHydrationExecutableSourceFields
import graphql.nadel.hints.NadelNoInterfaceToObjectFragmentExpansionHint
import graphql.nadel.hints.NadelReachableUnderlyingServiceTypesHint
import graphql.nadel.hints.NadelShadowUnderlyingTypeNameInvestigation
import graphql.nadel.hints.NadelSharedTypeRenamesHint

data class NadelExecutionHints(
    val legacyOperationNames: LegacyOperationNamesHint,
    val deferSupport: NadelDeferSupportHint,
    val sharedTypeRenames: NadelSharedTypeRenamesHint,
    val executeOnEngineSchema: NadelExecuteOnEngineSchemaHint,
    val hydrationExecutableSourceFields: NadelHydrationExecutableSourceFields,
    val shadowUnderlyingTypeNameInvestigation: NadelShadowUnderlyingTypeNameInvestigation,
    val disableSharedTypes: NadelDisableSharedTypesHint,
    val useReachableUnderlyingServiceTypes: NadelReachableUnderlyingServiceTypesHint,
    val batchRootFields: NadelBatchRootFieldsHint,
    val noInterfaceToObjectFragmentExpansion: NadelNoInterfaceToObjectFragmentExpansionHint,
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
        private var deferSupport = NadelDeferSupportHint { false }
        private var sharedTypeRenames = NadelSharedTypeRenamesHint { false }
        private var executeOnEngineSchema = NadelExecuteOnEngineSchemaHint { false }
        private var hydrationExecutableSourceFields = NadelHydrationExecutableSourceFields { false }
        private var shadowUnderlyingTypeNameInvestigation = NadelShadowUnderlyingTypeNameInvestigation { false }
        private var disableSharedTypes = NadelDisableSharedTypesHint { false }
        private var useReachableUnderlyingServiceTypes = NadelReachableUnderlyingServiceTypesHint { false }
        private var batchRootFields = NadelBatchRootFieldsHint { false }
        private var noInterfaceToObjectFragmentExpansion = NadelNoInterfaceToObjectFragmentExpansionHint { false }

        constructor()

        constructor(nadelExecutionHints: NadelExecutionHints) {
            legacyOperationNames = nadelExecutionHints.legacyOperationNames
            deferSupport = nadelExecutionHints.deferSupport
            sharedTypeRenames = nadelExecutionHints.sharedTypeRenames
            executeOnEngineSchema = nadelExecutionHints.executeOnEngineSchema
            hydrationExecutableSourceFields = nadelExecutionHints.hydrationExecutableSourceFields
            shadowUnderlyingTypeNameInvestigation = nadelExecutionHints.shadowUnderlyingTypeNameInvestigation
            disableSharedTypes = nadelExecutionHints.disableSharedTypes
            useReachableUnderlyingServiceTypes = nadelExecutionHints.useReachableUnderlyingServiceTypes
            batchRootFields = nadelExecutionHints.batchRootFields
            noInterfaceToObjectFragmentExpansion = nadelExecutionHints.noInterfaceToObjectFragmentExpansion
        }

        fun legacyOperationNames(flag: LegacyOperationNamesHint): Builder {
            legacyOperationNames = flag
            return this
        }

        fun deferSupport(flag: NadelDeferSupportHint): Builder {
            deferSupport = flag
            return this
        }

        fun sharedTypeRenames(flag: NadelSharedTypeRenamesHint): Builder {
            sharedTypeRenames = flag
            return this
        }

        fun executeOnEngineSchema(flag: NadelExecuteOnEngineSchemaHint): Builder {
            executeOnEngineSchema = flag
            return this
        }

        fun hydrationExecutableSourceFields(flag: NadelHydrationExecutableSourceFields): Builder {
            hydrationExecutableSourceFields = flag
            return this
        }

        fun shadowUnderlyingTypeNameInvestigation(flag: NadelShadowUnderlyingTypeNameInvestigation): Builder {
            shadowUnderlyingTypeNameInvestigation = flag
            return this
        }

        fun disableSharedTypes(flag: NadelDisableSharedTypesHint): Builder {
            disableSharedTypes = flag
            return this
        }

        fun useReachableUnderlyingServiceTypes(flag: NadelReachableUnderlyingServiceTypesHint): Builder {
            useReachableUnderlyingServiceTypes = flag
            return this
        }

        fun batchRootFields(flag: NadelBatchRootFieldsHint): Builder {
            batchRootFields = flag
            return this
        }

        fun noInterfaceToObjectFragmentExpansion(flag: NadelNoInterfaceToObjectFragmentExpansionHint): Builder {
            noInterfaceToObjectFragmentExpansion = flag
            return this
        }

        fun build(): NadelExecutionHints {
            return NadelExecutionHints(
                legacyOperationNames,
                deferSupport,
                sharedTypeRenames,
                executeOnEngineSchema,
                hydrationExecutableSourceFields,
                shadowUnderlyingTypeNameInvestigation,
                disableSharedTypes,
                useReachableUnderlyingServiceTypes,
                batchRootFields,
                noInterfaceToObjectFragmentExpansion,
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
