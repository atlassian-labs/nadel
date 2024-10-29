package graphql.nadel.validation

import graphql.Scalars.GraphQLID
import graphql.Scalars.GraphQLString
import graphql.language.UnionTypeDefinition
import graphql.nadel.Service
import graphql.nadel.definition.virtualType.isVirtualType
import graphql.nadel.engine.blueprint.NadelTypeRenameInstruction
import graphql.nadel.engine.util.AnyNamedNode
import graphql.nadel.engine.util.isExtensionDef
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.isNonNull
import graphql.nadel.engine.util.isNotWrapped
import graphql.nadel.engine.util.isWrapped
import graphql.nadel.engine.util.operationTypes
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.engine.util.unwrapOne
import graphql.nadel.validation.NadelSchemaValidationError.DuplicatedUnderlyingType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldOutputType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleType
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingType
import graphql.nadel.validation.util.NadelBuiltInTypes.allNadelBuiltInTypeNames
import graphql.nadel.validation.util.NadelSchemaUtil.getUnderlyingName
import graphql.nadel.validation.util.NadelSchemaUtil.getUnderlyingType
import graphql.nadel.validation.util.getReachableTypeNames
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLImplementingType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import graphql.schema.GraphQLUnionType
import graphql.schema.GraphQLUnmodifiedType

internal class NadelTypeValidation {
    private val fieldValidation = NadelFieldValidation(this)
    private val inputValidation = NadelInputValidation()
    private val unionValidation = NadelUnionValidation(this)
    private val enumValidation = NadelEnumValidation()
    private val interfaceValidation = NadelInterfaceValidation()
    private val namespaceValidation = NadelNamespaceValidation()

    context(NadelValidationContext)
    fun validate(
        service: Service,
    ): List<NadelSchemaValidationResult> {
        val (serviceTypes, serviceTypeErrors) = getServiceTypes(service)

        val reachableOverallTypeNames = serviceTypes
            .mapTo(HashSet()) { it.overall.name }
        val reachableUnderlyingTypeNames = serviceTypes
            .map { it.underlying.name }
        val definedUnderlyingTypeNames = service.underlyingSchema.typeMap.keys

        val typeValidationResult = serviceTypes.flatMap {
            validate(it)
        }

        val typeInstructions = serviceTypes
            .filter {
                it.overall.name != it.underlying.name
            }
            .filter {
                // Ignore scalar renames, refer to test "let jsw do jsw things"
                it.overall !is GraphQLScalarType
            }
            .map {
                NadelValidatedTypeResult(
                    service = service,
                    typeRenameInstruction = NadelTypeRenameInstruction(
                        service = service,
                        overallName = it.overall.name,
                        underlyingName = it.underlying.name,
                    )
                )
            }

        val serviceTypeMetadata = NadelTypenamesForService(
            service = service,
            overallTypeNames = reachableOverallTypeNames.toSet(),
            underlyingTypeNames = (reachableUnderlyingTypeNames + definedUnderlyingTypeNames).toSet(),
        )

        return serviceTypeErrors + typeValidationResult + serviceTypeMetadata + typeInstructions
    }

    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): List<NadelSchemaValidationResult> {
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
            unionValidation.validate(schemaElement) +
            interfaceValidation.validate(schemaElement) +
            namespaceValidation.validate(schemaElement) +
            enumValidation.validate(schemaElement)
    }

    context(NadelValidationContext)
    fun validateOutputType(
        parent: NadelServiceSchemaElement,
        overallField: GraphQLFieldDefinition,
        underlyingField: GraphQLFieldDefinition,
    ): List<NadelSchemaValidationResult> {
        val overallType = overallField.type.unwrapAll()
        val underlyingType = underlyingField.type.unwrapAll()

        val typeServiceSchemaElement = NadelServiceSchemaElement(
            service = parent.service,
            overall = overallType,
            underlying = underlyingType,
        )

        // This checks whether the type is actually valid content wise
        validate(typeServiceSchemaElement)
            .onError { return it }

        // This checks whether the output type e.g. name or List or NonNull wrappings are valid
        if (!isOutputTypeValid(overallType = overallField.type, underlyingType = underlyingField.type)) {
            return listOf(
                IncompatibleFieldOutputType(parent, overallField, underlyingField),
            )
        }

        return emptyList()
    }

    /**
     * Answers whether `rhs` assignable to `lhs`?
     *
     * i.e. does the following compile
     *
     * ```
     * vol output: lhs = rhs
     * ```
     *
     * Note: this assumes both types are from the same schema. This does NOT
     * deal with differences between overall and underlying schema.
     */
    context(NadelValidationContext)
    fun isAssignableTo(lhs: GraphQLNamedOutputType, rhs: GraphQLNamedOutputType): Boolean {
        if (lhs.name == rhs.name) {
            return true
        }
        if (lhs.name == GraphQLID.name && rhs.name == GraphQLString.name) {
            return true
        }
        if (lhs is GraphQLInterfaceType && rhs is GraphQLImplementingType) {
            return rhs.interfaces.contains(lhs)
        }
        return false
    }

    /**
     * It checks whether the type name and type wrappings e.g. [graphql.schema.GraphQLNonNull] make sense.
     */
    context(NadelValidationContext)
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

    context(NadelValidationContext)
    private fun isOutputTypeNameValid(
        overallType: GraphQLUnmodifiedType,
        underlyingType: GraphQLUnmodifiedType,
    ): Boolean {
        if (getUnderlyingName(overallType) == underlyingType.name) {
            return true
        }

        return false
    }

    context(NadelValidationContext)
    private fun getServiceTypes(
        service: Service,
    ): Pair<List<NadelServiceSchemaElement>, List<NadelSchemaValidationResult>> {
        val errors = mutableListOf<NadelSchemaValidationError>()
        val hydrationUnions = getHydrationUnions(service)
        val namesUsed = getTypeNamesUsed(service, externalTypes = hydrationUnions)

        fun addMissingUnderlyingTypeError(overallType: GraphQLNamedType) {
            errors.add(MissingUnderlyingType(service, overallType))
        }

        return namesUsed
            .map {
                engineSchema.typeMap[it]!!
            }
            .filterNot {
                it.name in allNadelBuiltInTypeNames
            }
            .mapNotNull { overallType ->
                val underlyingType = getUnderlyingType(overallType, service)

                if (underlyingType == null) {
                    if ((overallType as? GraphQLDirectiveContainer)?.isVirtualType() == true) {
                        // Do nothing
                    } else {
                        addMissingUnderlyingTypeError(overallType)
                    }
                    null
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

    context(NadelValidationContext)
    private fun getHydrationUnions(service: Service): List<GraphQLUnionType> {
        return service.definitionRegistry.definitions
            .asSequence()
            .filterIsInstance<UnionTypeDefinition>()
            .filter {
                it.name in hydrationUnions
            }
            .map {
                engineSchema.typeMap[it.name] as GraphQLUnionType
            }
            .toList()
    }

    context(NadelValidationContext)
    private fun getTypeNamesUsed(service: Service, externalTypes: List<GraphQLNamedType>): Set<String> {
        // There is no shared service to validate.
        // These shared types are USED in other services. When they are used, the validation
        // will validate that the service has a compatible underlying type.
        if (service.name == "shared") {
            return emptySet()
        }

        val namesToIgnore = externalTypes.mapTo(HashSet()) { it.name }

        val definitionNames = service.definitionRegistry.definitions
            .asSequence()
            .filterIsInstance<AnyNamedNode>()
            .filterNot {
                it.name in namesToIgnore
            }
            .filter { def ->
                if (def.isExtensionDef) {
                    isNamespacedOperationType(typeName = def.name) || isOperationType(typeName = def.name)
                } else {
                    true
                }
            }
            .map {
                it.name
            }
            .toList()

        val matchingImplementsNames = service.underlyingSchema.typeMap.values
            .asSequence()
            .filterIsInstance<GraphQLObjectType>()
            .filter {
                it.interfaces.isNotEmpty()
            }
            .map {
                it.name
            }
            .filter {
                engineSchema.typeMap[it] is GraphQLObjectType
            }

        // If it can be reached by using your service, you must own it to return it!
        return getReachableTypeNames(service, definitionNames + matchingImplementsNames)
    }

    context(NadelValidationContext)
    private fun isNamespacedOperationType(typeName: String): Boolean {
        return typeName in namespaceTypeNames
    }

    context(NadelValidationContext)
    private fun isOperationType(typeName: String): Boolean {
        return engineSchema.operationTypes.any { it.name == typeName }
    }

    context(NadelValidationContext)
    private fun visitElement(schemaElement: NadelServiceSchemaElement): Boolean {
        val schemaElementRef = schemaElement.toRef()

        return if (schemaElementRef in visitedTypes) {
            false // Already visited
        } else {
            visitedTypes.add(schemaElementRef)
            true
        }
    }
}
