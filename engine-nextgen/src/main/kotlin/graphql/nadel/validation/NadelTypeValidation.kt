package graphql.nadel.validation

import graphql.language.ObjectTypeDefinition
import graphql.nadel.Service
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
import graphql.nadel.schema.NadelDirectives.HYDRATED_DIRECTIVE_DEFINITION
import graphql.nadel.validation.NadelSchemaValidationError.DuplicatedUnderlyingType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldOutputType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleType
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingType
import graphql.nadel.validation.util.NadelBuiltInTypes.allNadelBuiltInTypeNames
import graphql.nadel.validation.util.NadelSchemaUtil.getUnderlyingName
import graphql.nadel.validation.util.NadelSchemaUtil.getUnderlyingType
import graphql.nadel.validation.util.getReachableTypeNames
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType
import graphql.schema.GraphQLUnionType
import graphql.schema.GraphQLUnmodifiedType

internal class NadelTypeValidation(
    private val context: NadelValidationContext,
    private val overallSchema: GraphQLSchema,
    services: Map<String, Service>,
    newHydrationValidation: Boolean,
) {
    private val fieldValidation = NadelFieldValidation(overallSchema, services, this, newHydrationValidation)
    private val inputValidation = NadelInputValidation()
    private val enumValidation = NadelEnumValidation()
    private val interfaceValidation = NadelInterfaceValidation()

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
            interfaceValidation.validate(schemaElement) +
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

        // This checks whether the type is actually valid content wise
        val typeErrors = validate(typeServiceSchemaElement)

        // This checks whether the output type e.g. name or List or NonNull wrappings are valid
        val outputTypeError = when {
            isOutputTypeValid(overallType = overallField.type, underlyingType = underlyingField.type) -> null
            else -> IncompatibleFieldOutputType(parent, overallField, underlyingField)
        }

        return typeErrors + listOfNotNull(outputTypeError)
    }

    /**
     * It checks whether the type name and type wrappings e.g. [graphql.schema.GraphQLNonNull] make sense.
     */
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
            return isOutputTypeNameValid(
                overallType = overall as GraphQLUnmodifiedType,
                underlyingType = underlying as GraphQLUnmodifiedType,
            )
        } else if (overall.isNotWrapped && underlying.isWrapped) {
            if (underlying.isNonNull && underlying.unwrapNonNull().isNotWrapped) {
                return isOutputTypeNameValid(
                    overallType = overall as GraphQLUnmodifiedType,
                    underlyingType = underlying.unwrapNonNull() as GraphQLUnmodifiedType,
                )
            }
            return false
        } else {
            return false
        }
    }

    private fun isOutputTypeNameValid(
        overallType: GraphQLUnmodifiedType,
        underlyingType: GraphQLUnmodifiedType,
    ): Boolean {
        if (getUnderlyingName(overallType) == underlyingType.name) {
            return true
        }

        // Ignore what the name is for scalars
        if (overallType is GraphQLScalarType && underlyingType is GraphQLScalarType) {
            return true
        }

        return false
    }

    private fun getServiceTypes(
        service: Service,
    ): Pair<List<NadelServiceSchemaElement>, List<NadelSchemaValidationError>> {
        val errors = mutableListOf<NadelSchemaValidationError>()
        val polymorphicHydrationUnions = getPolymorphicHydrationUnions(service)
        val namesUsed = getTypeNamesUsed(service, externalTypes = polymorphicHydrationUnions)

        fun addMissingUnderlyingTypeError(overallType: GraphQLNamedType) {
            errors.add(MissingUnderlyingType(service, overallType))
        }

        return overallSchema
            .typeMap
            .asSequence()
            .filter { (key) ->
                key in namesUsed
            }
            .filterNot { (key) ->
                key in allNadelBuiltInTypeNames
            }
            .mapNotNull { (_, overallType) ->
                val underlyingType = getUnderlyingType(overallType, service)

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

    private fun getPolymorphicHydrationUnions(service: Service): Set<GraphQLUnionType> {
        return service.definitionRegistry
            .definitions
            .asSequence()
            .filterIsInstance<ObjectTypeDefinition>()
            .flatMap { it.fieldDefinitions }
            .filter { it.getDirectives(HYDRATED_DIRECTIVE_DEFINITION.name).size > 1 }
            .map { it.type.unwrapAll() }
            .map { overallSchema.getType(it.name) }
            .filterIsInstance<GraphQLUnionType>()
            .toSet()
    }

    private fun getTypeNamesUsed(service: Service, externalTypes: Set<GraphQLNamedType>): Set<String> {
        // There is no shared service to validate.
        // These shared types are USED in other services. When they are used, the validation
        // will validate that the service has a compatible underlying type.
        if (service.name == "shared") {
            return emptySet()
        }

        val namesToIgnore = externalTypes.map { it.name }.toSet()

        val definitionNames = service.definitionRegistry.definitions
            .asSequence()
            .map { it as AnyNamedNode }
            .filter { def ->
                if (def.isExtensionDef) {
                    isNamespacedOperationType(typeName = def.name)
                } else {
                    true
                }
            }
            .map {
                it.name
            }
            .toSet() - namesToIgnore

        // If it can be reached by using your service, you must own it to return it!
        val referencedTypes = getReachableTypeNames(overallSchema, service, definitionNames)

        return (definitionNames + referencedTypes).toSet()
    }

    private fun isNamespacedOperationType(typeName: String): Boolean {
        return overallSchema.operationTypes.any { operationType ->
            operationType.fields.any { field ->
                field.hasDirective(NadelDirectives.NAMESPACED_DIRECTIVE_DEFINITION.name) &&
                    field.type.unwrapAll().name == typeName
            }
        }
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
