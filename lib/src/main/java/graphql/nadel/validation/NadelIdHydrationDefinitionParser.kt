package graphql.nadel.validation

import graphql.nadel.definition.hydration.NadelBatchObjectIdentifiedByDefinition
import graphql.nadel.definition.hydration.NadelDefaultHydrationDefinition
import graphql.nadel.definition.hydration.NadelHydrationArgumentDefinition
import graphql.nadel.definition.hydration.NadelHydrationConditionDefinition
import graphql.nadel.definition.hydration.NadelHydrationDefinition
import graphql.nadel.definition.hydration.NadelIdHydrationDefinition
import graphql.nadel.definition.hydration.parseDefaultHydrationOrNull
import graphql.nadel.definition.hydration.parseIdHydrationOrNull
import graphql.nadel.engine.util.unwrapAll
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLUnionType

internal class NadelIdHydrationDefinitionParser {
    fun parse(
        parent: GraphQLFieldsContainer,
        virtualField: GraphQLFieldDefinition,
    ): NadelValidationInterimResult<List<NadelHydrationDefinition>> {
        val idHydration = virtualField.parseIdHydrationOrNull()
            ?: return NadelValidationInterimResult.Success.of(emptyList())

        return getHydrationDefinitions(parent, virtualField, idHydration)
    }

    private fun getHydrationDefinitions(
        parent: GraphQLFieldsContainer,
        virtualField: GraphQLFieldDefinition,
        idHydration: NadelIdHydrationDefinition,
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

            NadelValidationInterimResult.Success.of(listOf(hydration))
        }
    }

    private fun getHydrationDefinitionsForUnion(
        virtualFieldType: GraphQLUnionType,
        parent: GraphQLFieldsContainer,
        virtualField: GraphQLFieldDefinition,
        idHydration: NadelIdHydrationDefinition,
    ): NadelValidationInterimResult<List<NadelHydrationDefinition>> {
        return NadelValidationInterimResult.Success.of(
            virtualFieldType.types
                .mapNotNull { unionMemberType ->
                    if ((unionMemberType as? GraphQLNamedType)?.parseDefaultHydrationOrNull() == null) {
                        null
                    } else {
                        getHydrationDefinitionForType(
                            parent,
                            virtualField,
                            idHydration,
                            unionMemberType as GraphQLObjectType,
                        ).onErrorCast { return it }
                    }
                },
        )
    }

    private fun getHydrationDefinitionForType(
        parent: GraphQLFieldsContainer,
        virtualField: GraphQLFieldDefinition,
        idHydration: NadelIdHydrationDefinition,
        type: GraphQLNamedType,
    ): NadelValidationInterimResult<NadelHydrationDefinition> {
        val defaultHydration = (type as? GraphQLNamedType)?.parseDefaultHydrationOrNull()
            ?: return NadelValidationInterimResult.Error.of(NadelMissingDefaultHydrationError(parent, virtualField))

        return NadelValidationInterimResult.Success.of(
            NadelIdHydratedHydrationDefinition(
                idHydration = idHydration,
                defaultHydration = defaultHydration,
            ),
        )
    }
}

internal class NadelIdHydratedHydrationDefinition(
    private val idHydration: NadelIdHydrationDefinition,
    private val defaultHydration: NadelDefaultHydrationDefinition,
) : NadelHydrationDefinition {
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
}
