package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.enginekt.util.AnyImplementingTypeDefinition
import graphql.nadel.enginekt.util.AnyNamedNode
import graphql.nadel.enginekt.util.isExtensionDef
import graphql.nadel.enginekt.util.unwrapAll
import graphql.nadel.validation.NadelSchemaUtil.getUnderlyingName
import graphql.nadel.validation.NadelSchemaUtil.getUnderlyingType
import graphql.nadel.validation.NadelSchemaUtil.hasHydration
import graphql.nadel.validation.NadelSchemaUtil.hasRename
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleTypeName
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingType
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType

internal class NadelTypeValidation(
    private val context: NadelValidationContext,
    private val overallSchema: GraphQLSchema,
    private val services: Map<String, Service>,
) {
    fun validate(
        service: Service,
    ): List<NadelSchemaValidationError> {
        val fieldValidation = NadelFieldValidation(overallSchema, services, service, this)
        val inputValidation = NadelInputValidation(overallSchema, services, service)
        val enumValidation = NadelEnumValidation(overallSchema, services, service)

        val (serviceTypes, serviceTypeErrors) = getServiceTypes(service)

        val fieldIssues = serviceTypes.asSequence()
            .filter(::visitElement)
            .flatMap { serviceType ->
                // Since these are GraphQL executable types, they should be the same
                // i.e. there's no GraphQLExtendedFieldDefinition to screw it up
                if (serviceType.overall.javaClass != serviceType.underlying.javaClass) {
                    listOf(
                        IncompatibleType(serviceType),
                    )
                } else {
                    fieldValidation.validate(serviceType) +
                        inputValidation.validate(serviceType) +
                        enumValidation.validate(serviceType)
                }
            }
            .toList()

        return serviceTypeErrors + fieldIssues
    }

    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationError> {
        if (!visitElement(schemaElement)) {
            return emptyList()
        }

        if (schemaElement.overall.javaClass != schemaElement.underlying.javaClass) {
            return listOf(
                IncompatibleType(schemaElement),
            )
        }

        if (schemaElement.overall is GraphQLFieldsContainer) {
            val underlyingTypeName = getUnderlyingName(schemaElement.overall as GraphQLType)
                ?: schemaElement.overall.name
            // This mismatch happens when field output types are validated
            if (underlyingTypeName != schemaElement.underlying.name) {
                return listOf(
                    IncompatibleTypeName(schemaElement),
                )
            }
        }

        val fieldValidation = NadelFieldValidation(overallSchema, services, schemaElement.service, this)
        return fieldValidation.validate(schemaElement)
    }

    private fun getServiceTypes(
        service: Service,
    ): Pair<List<NadelServiceSchemaElement>, List<NadelSchemaValidationError>> {
        val errors = mutableListOf<NadelSchemaValidationError>()
        val nameNamesUsed = getTypeNamesUsed(service)

        fun addMissingUnderlyingTypeError(overallType: GraphQLNamedType) {
            errors.add(MissingUnderlyingType(service, overallType))
        }

        return overallSchema
            .typeMap
            .asSequence()
            .filter { (key) ->
                key in nameNamesUsed
            }
            .mapNotNull { (_, overallType) ->
                val underlyingType = getUnderlyingType(overallType, service) as GraphQLNamedType?
                if (underlyingType == null) {
                    addMissingUnderlyingTypeError(overallType).let { null }
                } else {
                    NadelServiceSchemaElement(
                        service = service,
                        overall = overallType,
                        underlying = underlyingType,
                    )
                }
            }
            .toList() to errors
    }

    private fun getTypeNamesUsed(service: Service): Set<String> {
        // There is no shared service to validate.
        // These shared types are USED in other services. When they are used, the validation
        // will validate that the service has a compatible underlying type.
        if (service.name == "shared") {
            return emptySet()
        }

        val definitionNames = service.definitionRegistry.definitions.asSequence()
            .filterNot { it.isExtensionDef }
            .map {
                it as AnyNamedNode
                it.name
            }
        val definitionsNamesReferencedAsOutputTypes = service.definitionRegistry.definitions
            .asSequence()
            // i.e. has fields
            .filterIsInstance<AnyImplementingTypeDefinition>()
            .flatMap {
                it.fieldDefinitions
            }
            .filterNot {
                hasHydration(it) || hasRename(it)
            }
            .map {
                it.type.unwrapAll().name
            }

        return (definitionNames + definitionsNamesReferencedAsOutputTypes).toSet()
    }

    private fun visitElement(schemaElement: NadelServiceSchemaElement): Boolean {
        val schemaElementRef = schemaElement.toRef()

        return if (schemaElementRef in context.visitedTypes) {
            false // Already visited
        } else {
            context.visitedTypes.add(schemaElementRef)
            true
        }
    }
}
