package graphql.nadel.validation

import graphql.nadel.Service
import graphql.nadel.enginekt.util.AnyImplementingTypeDefinition
import graphql.nadel.enginekt.util.AnyNamedNode
import graphql.nadel.enginekt.util.isExtensionDef
import graphql.nadel.enginekt.util.isList
import graphql.nadel.enginekt.util.isNonNull
import graphql.nadel.enginekt.util.isNotWrapped
import graphql.nadel.enginekt.util.isWrapped
import graphql.nadel.enginekt.util.operationTypes
import graphql.nadel.enginekt.util.unwrapAll
import graphql.nadel.enginekt.util.unwrapNonNull
import graphql.nadel.enginekt.util.unwrapOne
import graphql.nadel.schema.NadelDirectives
import graphql.nadel.validation.NadelSchemaUtil.getUnderlyingName
import graphql.nadel.validation.NadelSchemaUtil.getUnderlyingType
import graphql.nadel.validation.NadelSchemaUtil.hasHydration
import graphql.nadel.validation.NadelSchemaUtil.hasRename
import graphql.nadel.validation.NadelSchemaValidationError.DuplicatedUnderlyingType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldOutputType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldOutputTypeName
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleType
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingType
import graphql.schema.GraphQLEnumType
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType
import graphql.schema.GraphQLUnmodifiedType

internal class NadelTypeValidation(
    private val context: NadelValidationContext,
    private val overallSchema: GraphQLSchema,
    services: Map<String, Service>,
) {
    private val fieldValidation = NadelFieldValidation(overallSchema, services, this)
    private val inputValidation = NadelInputValidation()
    private val enumValidation = NadelEnumValidation()

    fun validate(
        service: Service,
    ): List<NadelSchemaValidationError> {
        val (serviceTypes, serviceTypeErrors) = getServiceTypes(service)

        val fieldIssues = serviceTypes.asSequence()
            .flatMap(::validate)
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

        return fieldValidation.validate(schemaElement) +
            inputValidation.validate(schemaElement) +
            enumValidation.validate(schemaElement)
    }

    fun validateOutputType(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        underlyingField: GraphQLFieldDefinition,
    ): List<NadelSchemaValidationError> {
        val overallType = overallField.type.unwrapAll()
        val underlyingType = underlyingField.type.unwrapAll()

        val typeServiceSchemaElement = NadelServiceSchemaElement(
            service = parent.service,
            overall = overallType,
            underlying = underlyingType,
        )

        val underlyingTypeName = getUnderlyingName(overallType)
        // This mismatch happens when field output types are validated
        if (underlyingTypeName != underlyingType.name) {
            if (overallType is GraphQLScalarType && underlyingType is GraphQLScalarType) {
            } else if (overallType is GraphQLEnumType && underlyingType is GraphQLScalarType) {
            } else if (overallType is GraphQLScalarType && underlyingType is GraphQLEnumType) {
            } else {
                return listOf(
                    IncompatibleFieldOutputTypeName(parent, overallField, underlyingField),
                )
            }
        }

        val outputTypeWrappingErrors = listOfNotNull(
            validateOutputTypeWrapping(parent, overallField, underlyingField),
        )

        return validate(typeServiceSchemaElement) + outputTypeWrappingErrors
    }

    private fun validateOutputTypeWrapping(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        underlyingField: GraphQLFieldDefinition,
    ): NadelSchemaValidationError? {
        return if (isOutputTypeValid(overallType = overallField.type, underlyingType = underlyingField.type)) {
            null
        } else {
            IncompatibleFieldOutputType(parent, overallField, underlyingField)
        }
    }

    private fun isOutputTypeValid(
        overallType: GraphQLOutputType,
        underlyingType: GraphQLOutputType,
    ): Boolean {
        var overall: GraphQLType = overallType
        var underlying: GraphQLType = underlyingType

        while (overall.isWrapped && underlying.isWrapped) {
            if (underlying.isNonNull && !overall.isNonNull) {
                // Overall type is allowed to have looser restrictions
                underlying = underlying.unwrapOne()
            } else if ((overall.isList && underlying.isList) || (overall.isNonNull && underlying.isNonNull)) {
                overall = overall.unwrapOne()
                underlying = underlying.unwrapOne()
            } else {
                return false
            }
        }

        if (overall.isNotWrapped && underlying.isNotWrapped) {
            return getUnderlyingName(overall as GraphQLUnmodifiedType) == (underlying as GraphQLUnmodifiedType).name
        } else if (overall.isNotWrapped && underlying.isWrapped) {
            if (underlying.isNonNull && underlying.unwrapNonNull().isNotWrapped) {
                return getUnderlyingName(overall as GraphQLUnmodifiedType) == (underlying.unwrapNonNull() as GraphQLUnmodifiedType).name
            }
            return false
        } else {
            return false
        }
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
            .toList()
            .also { types ->
                // Add error for duplicated types
                errors.addAll(
                    types
                        .groupBy { it.underlying.name }
                        .filterValues { it.size > 1 }
                        .values
                        .map { duplicatedTypes ->
                            DuplicatedUnderlyingType(duplicatedTypes)
                        },
                )
            } to errors
    }

    private fun getTypeNamesUsed(service: Service): Set<String> {
        // There is no shared service to validate.
        // These shared types are USED in other services. When they are used, the validation
        // will validate that the service has a compatible underlying type.
        if (service.name == "shared") {
            return emptySet()
        }

        val definitionNames = service.definitionRegistry.definitions.asSequence()
            .map { it as AnyNamedNode }
            .filter { def ->
                if (def.isExtensionDef) {
                    // Include extensions if it's a namespaced type
                    overallSchema.operationTypes.any { operationType ->
                        operationType.fields.any { field ->
                            field.hasDirective(NadelDirectives.NAMESPACED_DIRECTIVE_DEFINITION.name) &&
                                field.type.unwrapAll().name == def.name
                        }
                    }
                } else {
                    true
                }
            }
            .map {
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
