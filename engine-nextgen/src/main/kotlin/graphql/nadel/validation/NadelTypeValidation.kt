package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.enginekt.util.AnyImplementingTypeDefinition
import graphql.nadel.enginekt.util.AnyNamedNode
import graphql.nadel.enginekt.util.isExtensionDef
import graphql.nadel.enginekt.util.unwrapAll
import graphql.nadel.validation.NadelSchemaUtil.getUnderlyingType
import graphql.nadel.validation.NadelSchemaUtil.hasHydration
import graphql.nadel.validation.NadelSchemaUtil.hasRename
import graphql.nadel.validation.NadelSchemaValidationError.Companion.missingUnderlyingType
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLSchema

class NadelTypeValidation(
    private val context: NadelValidationContext,
    private val overallSchema: GraphQLSchema,
    private val services: Map<String, Service>,
) {
    fun getIssues(
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
                        NadelSchemaValidationError.incompatibleType(serviceType),
                    )
                } else {
                    fieldValidation.getIssues(serviceType) +
                        inputValidation.getIssues(serviceType) +
                        enumValidation.getIssues(serviceType)
                }
            }
            .toList()

        return serviceTypeErrors + fieldIssues
    }

    fun getIssues(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationError> {
        if (!visitElement(schemaElement)) {
            return emptyList()
        }

        if (schemaElement.overall.javaClass != schemaElement.underlying.javaClass) {
            return listOf(
                NadelSchemaValidationError.incompatibleType(schemaElement),
            )
        }

        val fieldValidation = NadelFieldValidation(overallSchema, services, schemaElement.service, this)
        return fieldValidation.getIssues(schemaElement)
    }

    private fun getServiceTypes(
        service: Service,
    ): Pair<List<NadelServiceSchemaElement>, List<NadelSchemaValidationError>> {
        val errors = mutableListOf<NadelSchemaValidationError>()
        val nameNamesUsed = getTypeNamesUsed(service)

        fun addMissingUnderlyingTypeError(overallType: GraphQLNamedType) {
            errors.add(missingUnderlyingType(service, overallType.name))
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
        // Types here do not belong to any service, so there is no validation to be had
        // When the shared types get referenced, they will be validated against that service
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
