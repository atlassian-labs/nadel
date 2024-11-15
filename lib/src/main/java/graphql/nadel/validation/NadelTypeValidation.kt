package graphql.nadel.validation

import graphql.Scalars.GraphQLID
import graphql.Scalars.GraphQLString
import graphql.language.UnionTypeDefinition
import graphql.nadel.Service
import graphql.nadel.definition.virtualType.isVirtualType
import graphql.nadel.engine.blueprint.NadelTypeRenameInstruction
import graphql.nadel.engine.util.AnyNamedNode
import graphql.nadel.engine.util.isExtensionDef
import graphql.nadel.engine.util.operationTypes
import graphql.nadel.validation.NadelSchemaValidationError.DuplicatedUnderlyingType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleType
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingType
import graphql.nadel.validation.util.NadelBuiltInTypes
import graphql.nadel.validation.util.NadelBuiltInTypes.allNadelBuiltInTypeNames
import graphql.nadel.validation.util.NadelSchemaUtil.getUnderlyingType
import graphql.nadel.validation.util.getReachableTypeNames
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLImplementingType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLUnionType

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
    ): NadelSchemaValidationResult {
        val (serviceTypes, serviceTypeErrors) = getServiceTypes(service)

        val serviceTypeMetadata = getReachableTypeMetadata(serviceTypes, service)

        val typeValidationResult = serviceTypes
            .map {
                validate(it)
            }
            .toResult()

        return results(
            serviceTypeErrors,
            typeValidationResult,
            serviceTypeMetadata,
        )
    }

    private fun getReachableTypeMetadata(
        serviceTypes: List<NadelServiceSchemaElement>,
        service: Service,
    ): NadelReachableServiceTypesResult {
        val reachableOverallTypeNames = serviceTypes
            .mapTo(mutableSetOf()) { it.overall.name }
        val reachableUnderlyingTypeNames = serviceTypes
            .map { it.underlying.name }

        val definedUnderlyingTypeNames = service.underlyingSchema.typeMap.keys

        return NadelReachableServiceTypesResult(
            service = service,
            overallTypeNames = reachableOverallTypeNames,
            underlyingTypeNames = (reachableUnderlyingTypeNames + definedUnderlyingTypeNames).toSet(),
        )
    }

    context(NadelValidationContext)
    fun validate(
        schemaElement: NadelServiceSchemaElement,
    ): NadelSchemaValidationResult {
        if (!visitElement(schemaElement)) {
            return ok()
        }

        if (schemaElement.overall.javaClass != schemaElement.underlying.javaClass) {
            return IncompatibleType(schemaElement)
        }

        return results(
            validateTypeRename(schemaElement),
            fieldValidation.validate(schemaElement),
            inputValidation.validate(schemaElement),
            unionValidation.validate(schemaElement),
            interfaceValidation.validate(schemaElement),
            namespaceValidation.validate(schemaElement),
            enumValidation.validate(schemaElement),
        )
    }

    context(NadelValidationContext)
    private fun validateTypeRename(
        type: NadelServiceSchemaElement,
    ): NadelSchemaValidationResult {
        if (type.overall.name == type.underlying.name) {
            return ok()
        }

        // Ignore scalar renames, refer to test "let jsw do jsw things"
        if (type.overall is GraphQLScalarType) {
            return ok()
        }

        return NadelValidatedTypeResult(
            typeRenameInstruction = NadelTypeRenameInstruction(
                service = type.service,
                overallName = type.overall.name,
                underlyingName = type.underlying.name,
            ),
        )
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

    context(NadelValidationContext)
    private fun getServiceTypes(
        service: Service,
    ): Pair<List<NadelServiceSchemaElement>, NadelSchemaValidationResult> {
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
            } to errors.toResult()
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
            .filterNot {
                it.name in NadelBuiltInTypes.builtInDirectiveSyntaxTypeNames
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
