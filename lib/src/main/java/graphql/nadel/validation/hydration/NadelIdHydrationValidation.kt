package graphql.nadel.validation.hydration

import graphql.nadel.definition.hydration.NadelBatchObjectIdentifiedByDefinition
import graphql.nadel.definition.hydration.NadelHydrationArgumentDefinition
import graphql.nadel.definition.hydration.NadelHydrationConditionDefinition
import graphql.nadel.definition.hydration.NadelHydrationDefinition
import graphql.nadel.definition.hydration.NadelIdHydrationDirectiveDefinition
import graphql.nadel.definition.hydration.getDefaultHydrationOrNull
import graphql.nadel.definition.hydration.getIdHydrationOrNull
import graphql.nadel.definition.renamed.isRenamed
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.validation.NadelMissingDefaultHydrationError
import graphql.nadel.validation.NadelSchemaValidationError.CannotRenameHydratedField
import graphql.nadel.validation.NadelSchemaValidationResult
import graphql.nadel.validation.NadelServiceSchemaElement
import graphql.nadel.validation.NadelValidationContext
import graphql.nadel.validation.NadelValidationInterimResult
import graphql.nadel.validation.NadelValidationInterimResult.Error.Companion.asInterimError
import graphql.nadel.validation.NadelValidationInterimResult.Success.Companion.asInterimSuccess
import graphql.nadel.validation.onError
import graphql.nadel.validation.onErrorCast
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLUnionType

internal class NadelIdHydrationValidation(
    private val hydrationValidation: NadelHydrationValidation,
) {
    context(NadelValidationContext)
    fun validate(
        parent: NadelServiceSchemaElement.FieldsContainer,
        virtualField: GraphQLFieldDefinition,
    ): NadelSchemaValidationResult {
        if (virtualField.isRenamed()) {
            return CannotRenameHydratedField(parent, virtualField)
        }

        val idHydration = virtualField.getIdHydrationOrNull()
            ?: error("Don't invoke ID hydration validation if there is no ID hydration silly")

        return hydrationValidation.validate(
            parent = parent,
            virtualField = virtualField,
            hydrations = getHydrationDefinitions(
                parent = parent,
                virtualField = virtualField,
                idHydration = idHydration,
            ).onError { return it },
        )
    }

    context(NadelValidationContext)
    private fun getHydrationDefinitions(
        parent: NadelServiceSchemaElement,
        virtualField: GraphQLFieldDefinition,
        idHydration: NadelIdHydrationDirectiveDefinition,
    ): NadelValidationInterimResult<List<NadelHydrationDefinition>> {
        val virtualFieldType = virtualField.type.unwrapAll()

        return if (virtualFieldType is GraphQLUnionType) {
            getHydrationDefinitionsForUnion(
                virtualFieldType = virtualFieldType,
                parent = parent,
                virtualField = virtualField,
                idHydration = idHydration
            )
        } else {
            val hydration = getHydrationDefinitionForType(
                parent = parent,
                virtualField = virtualField,
                idHydration = idHydration,
                type = virtualFieldType,
            ).onErrorCast { return it }

            listOf(hydration).asInterimSuccess()
        }
    }

    context(NadelValidationContext)
    private fun getHydrationDefinitionsForUnion(
        virtualFieldType: GraphQLUnionType,
        parent: NadelServiceSchemaElement,
        virtualField: GraphQLFieldDefinition,
        idHydration: NadelIdHydrationDirectiveDefinition,
    ): NadelValidationInterimResult<List<NadelHydrationDefinition>> {
        return virtualFieldType.types
            .map { unionMemberType ->
                getHydrationDefinitionForType(
                    parent,
                    virtualField,
                    idHydration,
                    unionMemberType as GraphQLObjectType,
                ).onErrorCast { return it }
            }
            .asInterimSuccess()
    }

    context(NadelValidationContext)
    private fun getHydrationDefinitionForType(
        parent: NadelServiceSchemaElement,
        virtualField: GraphQLFieldDefinition,
        idHydration: NadelIdHydrationDirectiveDefinition,
        type: GraphQLNamedType,
    ): NadelValidationInterimResult<NadelHydrationDefinition> {
        val defaultHydration = (type as? GraphQLNamedType)?.getDefaultHydrationOrNull()
            ?: return NadelMissingDefaultHydrationError(parent, virtualField).asInterimError()

        return object : NadelHydrationDefinition {
            override val backingField: List<String>
                get() = defaultHydration.backingField

            override val identifiedBy: String?
                get() = idHydration.identifiedBy ?: defaultHydration.identifiedBy

            override val isIndexed: Boolean
                get() = false

            override val batchSize: Int
                get() = defaultHydration.batchSize

            override val arguments: List<NadelHydrationArgumentDefinition>
                get() = listOf(
                    NadelHydrationArgumentDefinition.ObjectField(
                        name = defaultHydration.idArgument,
                        pathToField = idHydration.idField,
                    ),
                )
            override val condition: NadelHydrationConditionDefinition?
                get() = null

            override val timeout: Int
                get() = defaultHydration.timeout

            override val inputIdentifiedBy: List<NadelBatchObjectIdentifiedByDefinition>?
                get() = null
        }.asInterimSuccess()
    }
}
