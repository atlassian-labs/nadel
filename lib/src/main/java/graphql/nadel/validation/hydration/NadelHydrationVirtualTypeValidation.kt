package graphql.nadel.validation.hydration

import graphql.nadel.definition.hydration.isHydrated
import graphql.nadel.definition.virtualType.isVirtualType
import graphql.nadel.engine.blueprint.NadelVirtualTypeContext
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.validation.NadelValidationContext
import graphql.nadel.validation.NadelValidationInterimResult
import graphql.nadel.validation.NadelValidationInterimResult.Success.Companion.asInterimSuccess
import graphql.nadel.validation.onError
import graphql.nadel.validation.onErrorCast
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLNamedType

private data class NadelVirtualTypeMapping(
    val virtualType: GraphQLNamedType,
    val backingType: GraphQLNamedType,
)

private class NadelHydrationVirtualTypeValidationContext {
    private val visited = mutableSetOf<Pair<String, String>>()

    /**
     * @return true to continue
     */
    fun visit(mapping: NadelVirtualTypeMapping): Boolean {
        val reference = mapping.virtualType.name to mapping.backingType.name
        return visited.add(reference)
    }
}

/**
 * This only generates a [NadelVirtualTypeContext] assuming a valid virtual type.
 *
 * A virtual types are validated in [graphql.nadel.validation.NadelVirtualTypeValidation]
 */
internal class NadelHydrationVirtualTypeValidation {
    private val emptyMapping = emptyList<NadelVirtualTypeMapping>()

    context(NadelValidationContext, NadelHydrationValidationContext)
    fun getVirtualTypeContext(): NadelValidationInterimResult<NadelVirtualTypeContext?> {
        // Do nothing if it's not a virtual type
        if (!virtualField.type.unwrapAll().isVirtualType()) {
            return null.asInterimSuccess()
        }

        val context = NadelHydrationVirtualTypeValidationContext()
        val typeMappings = with(context) {
            getVirtualTypeMapping(
                virtualField = virtualField,
                backingField = backingField,
            ).onErrorCast { return it }
        }

        return NadelVirtualTypeContext(
            virtualFieldContainer = parent.overall,
            virtualField = virtualField,
            virtualTypeToBackingType = typeMappings.associate {
                it.virtualType.name to it.backingType.name
            },
            backingTypeToVirtualType = typeMappings.associate {
                it.backingType.name to it.virtualType.name
            },
        ).asInterimSuccess()
    }

    context(NadelValidationContext, NadelHydrationVirtualTypeValidationContext)
    private fun getVirtualTypeMapping(
        virtualField: GraphQLFieldDefinition,
        backingField: GraphQLFieldDefinition,
    ): NadelValidationInterimResult<List<NadelVirtualTypeMapping>> {
        val virtualFieldOutputType = virtualField.type.unwrapAll()
        val backingFieldOutputType = backingField.type.unwrapAll()

        val mapping = NadelVirtualTypeMapping(
            virtualType = virtualFieldOutputType,
            backingType = backingFieldOutputType,
        )

        if (!visit(mapping)) {
            return emptyMapping.asInterimSuccess()
        }

        val childMappings = getVirtualTypeMappings(
            virtualFieldOutputType,
            backingFieldOutputType,
        ).onError { return it }

        return (listOf(mapping) + childMappings).asInterimSuccess()
    }

    context(NadelValidationContext, NadelHydrationVirtualTypeValidationContext)
    private fun getVirtualTypeMappings(
        virtualObjectType: GraphQLNamedType,
        backingObjectType: GraphQLNamedType,
    ): NadelValidationInterimResult<List<NadelVirtualTypeMapping>> {
        if (virtualObjectType is GraphQLFieldsContainer && backingObjectType is GraphQLFieldsContainer) {
            return virtualObjectType.fields
                .flatMap { virtualField ->
                    if (virtualField.isHydrated()) {
                        emptyMapping
                    } else {
                        val backingField = backingObjectType.getField(virtualField.name)
                        if (backingField == null) {
                            emptyMapping
                        } else {
                            getVirtualTypeMapping(
                                virtualField = virtualField,
                                backingField = backingField,
                            ).onError { return it }
                        }
                    }
                }
                .asInterimSuccess()
        }

        return emptyMapping.asInterimSuccess()
    }
}
