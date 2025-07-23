package graphql.nadel.validation

import graphql.nadel.validation.NadelSchemaValidationError.MissingConcreteTypes
import graphql.schema.GraphQLInterfaceType

class NadelInterfaceValidation internal constructor() {
    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement.Interface,
    ): NadelSchemaValidationResult {
        return validateHasImplementations(schemaElement)
    }

    context(NadelValidationContext)
    private fun validateHasImplementations(
        schemaElement: NadelServiceSchemaElement.Interface,
    ): NadelSchemaValidationResult {
        val underlyingSchema = schemaElement.service.underlyingSchema
        val underlyingInterfaceType = schemaElement.underlying
        val underlyingImplementations = underlyingSchema.getImplementations(underlyingInterfaceType)

        return if (underlyingImplementations.isEmpty()) {
            MissingConcreteTypes(schemaElement)
        } else {
            ok()
        }
    }

    context(NadelValidationContext)
    fun validateImplements(
        schemaElement: NadelServiceSchemaElement.ImplementingType,
    ): NadelSchemaValidationResult {
        val overallInterfaces = schemaElement.overall.interfaces
        val underlyingInterfaces = schemaElement.underlying.interfaces

        return overallInterfaces
            .asSequence()
            .map { untypedOverallInterface ->
                // This will be a real interface by the time the schema is resolved
                untypedOverallInterface as GraphQLInterfaceType
            }
            .filter { overallInterface ->
                // Find offending (missing) interfaces
                val underlyingInterfaceName = instructionDefinitions.getUnderlyingTypeName(overallInterface)
                underlyingInterfaces.none { it.name == underlyingInterfaceName }
            }
            .map { overallInterface ->
                NadelTypeMissingInterfaceError(schemaElement, overallInterface)
            }
            .toResult()
    }
}
