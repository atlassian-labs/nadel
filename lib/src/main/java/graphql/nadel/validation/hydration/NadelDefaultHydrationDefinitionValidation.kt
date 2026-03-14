package graphql.nadel.validation.hydration

import graphql.nadel.definition.coordinates.coordinates
import graphql.nadel.definition.hydration.NadelDefaultHydrationDefinition
import graphql.nadel.engine.util.getFieldAt
import graphql.nadel.engine.util.getFieldContainerFor
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.engine.util.whenType
import graphql.nadel.validation.NadelDefaultHydrationExceedsMaxBatchSizeError
import graphql.nadel.validation.NadelDefaultHydrationFieldNotFoundError
import graphql.nadel.validation.NadelDefaultHydrationIdArgumentNotFoundError
import graphql.nadel.validation.NadelDefaultHydrationIncompatibleBackingFieldTypeError
import graphql.nadel.validation.NadelSchemaValidationResult
import graphql.nadel.validation.NadelServiceSchemaElement
import graphql.nadel.validation.NadelValidationContext
import graphql.nadel.validation.ok

/**
 * Validates a `@defaultHydration` before it's actually put to use.
 */
class NadelDefaultHydrationDefinitionValidation {
    context(NadelValidationContext)
    fun validate(type: NadelServiceSchemaElement.Type): NadelSchemaValidationResult {
        val defaultHydration = instructionDefinitions.getDefaultHydrationOrNull(type)
            ?: return ok()

        return validate(type, defaultHydration)
    }

    context(NadelValidationContext)
    private fun validate(
        type: NadelServiceSchemaElement.Type,
        defaultHydration: NadelDefaultHydrationDefinition,
    ): NadelSchemaValidationResult {
        val backingField = engineSchema.queryType.getFieldAt(defaultHydration.backingField)
            ?: return NadelDefaultHydrationFieldNotFoundError(type, defaultHydration)

        val backingFieldContainer = engineSchema.queryType.getFieldContainerFor(defaultHydration.backingField)!!

        val backingFieldOutputType = backingField.type.unwrapAll()

        // Backing field should return the type declaring the @defaultHydration
        // It's ok if the backing field returns an abstract type and there are other possible options
        // We have validation elsewhere that dictates that the hydrated field type must be valid
        val backingFieldOutputTypeValid = backingFieldOutputType.whenType(
            enumType = { enumType -> enumType.name == type.overall.name },
            inputObjectType = { inputObjectType -> inputObjectType.name == type.overall.name },
            interfaceType = { interfaceType ->
                interfaceType.name == type.overall.name
                    || engineSchema.getImplementations(interfaceType).contains(type.overall)
            },
            objectType = { objectType -> objectType.name == type.overall.name },
            scalarType = { scalarType -> scalarType.name == type.overall.name },
            unionType = { unionType ->
                unionType.name == type.overall.name
                    || unionType.types.contains(type.overall)
            },
        )

        if (!backingFieldOutputTypeValid) {
            return NadelDefaultHydrationIncompatibleBackingFieldTypeError(type, defaultHydration, backingField)
        }

        if (backingField.getArgument(defaultHydration.idArgument) == null) {
            return NadelDefaultHydrationIdArgumentNotFoundError(type, defaultHydration, backingField)
        }

        val backingFieldCoordinates = backingFieldContainer.coordinates().field(backingField.name)
        val maxBatchSize = instructionDefinitions.getMaxBatchSizeOrNull(backingFieldCoordinates)?.size
        if (maxBatchSize != null && defaultHydration.batchSize > maxBatchSize) {
            return NadelDefaultHydrationExceedsMaxBatchSizeError(
                type = type,
                defaultHydration = defaultHydration,
                backingField = backingField,
                requestedBatchSize = defaultHydration.batchSize,
                maxBatchSize = maxBatchSize,
            )
        }

        return ok()
    }
}
