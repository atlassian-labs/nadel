package graphql.nadel.validation

import graphql.language.UnionTypeDefinition
import graphql.nadel.Service
import graphql.nadel.engine.blueprint.NadelTypeRenameInstruction
import graphql.nadel.engine.util.AnyNamedNode
import graphql.nadel.engine.util.isExtensionDef
import graphql.nadel.engine.util.operationTypes
import graphql.nadel.validation.NadelSchemaValidationError.DuplicatedUnderlyingType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleType
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingType
import graphql.nadel.validation.util.NadelBuiltInTypes.allNadelBuiltInTypeNames
import graphql.nadel.validation.util.NadelReferencedType
import graphql.nadel.validation.util.NadelSchemaUtil.getUnderlyingType
import graphql.nadel.validation.util.getReferencedTypeNames
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLUnionType

internal class NadelTypeValidation(
    private val fieldValidation: NadelFieldValidation,
    private val inputValidation: NadelInputObjectValidation,
    private val unionValidation: NadelUnionValidation,
    private val enumValidation: NadelEnumValidation,
    private val interfaceValidation: NadelInterfaceValidation,
    private val namespaceValidation: NadelNamespaceValidation,
    private val virtualTypeValidation: NadelVirtualTypeValidation,
) {
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

        val fieldsContainerResult = if (schemaElement is NadelServiceSchemaElement.FieldsContainer) {
            fieldValidation.validate(schemaElement)
        } else {
            ok()
        }

        val renameResult = if (schemaElement is NadelServiceSchemaElement.Type) {
            results(
                validateTypeRename(schemaElement),
                namespaceValidation.validate(schemaElement),
            )
        } else {
            ok()
        }

        val typeSpecificResult = when (schemaElement) {
            is NadelServiceSchemaElement.Enum -> {
                enumValidation.validate(schemaElement)
            }
            is NadelServiceSchemaElement.Interface -> {
                interfaceValidation.validate(schemaElement)
            }
            is NadelServiceSchemaElement.Object -> {
                ok()
            }
            is NadelServiceSchemaElement.InputObject -> {
                inputValidation.validate(schemaElement)
            }
            is NadelServiceSchemaElement.Scalar -> {
                ok()
            }
            is NadelServiceSchemaElement.Union -> {
                unionValidation.validate(schemaElement)
            }
            is NadelServiceSchemaElement.Incompatible -> {
                IncompatibleType(schemaElement)
            }
            is NadelServiceSchemaElement.VirtualType -> {
                virtualTypeValidation.validate(schemaElement)
            }
        }

        return results(
            fieldsContainerResult,
            renameResult,
            typeSpecificResult,
        )
    }

    context(NadelValidationContext)
    private fun validateTypeRename(
        type: NadelServiceSchemaElement.Type,
    ): NadelSchemaValidationResult {
        if (type.overall.name == type.underlying.name) {
            return ok()
        }

        // Ignore scalar renames, refer to test "let jsw do jsw things"
        if (type is NadelServiceSchemaElement.Scalar) {
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

    context(NadelValidationContext)
    private fun getServiceTypes(
        service: Service,
    ): Pair<List<NadelServiceSchemaElement>, NadelSchemaValidationResult> {
        val errors = mutableListOf<NadelSchemaValidationError>()
        val hydrationUnions = getHydrationUnions(service)
        val referencedTypes = getReferencedTypes(service, externalTypes = hydrationUnions)

        fun addMissingUnderlyingTypeError(overallType: GraphQLNamedType) {
            errors.add(MissingUnderlyingType(service, overallType))
        }

        return referencedTypes
            .filterNot {
                it.name in allNadelBuiltInTypeNames
            }
            .mapNotNull { referencedType ->
                when (referencedType) {
                    is NadelReferencedType.OrdinaryType -> {
                        val overallType = engineSchema.typeMap[referencedType.name]!!
                        val underlyingType = getUnderlyingType(overallType, service)

                        if (underlyingType == null) {
                            addMissingUnderlyingTypeError(overallType)
                            null
                        } else {
                            NadelServiceSchemaElement.from(
                                service = service,
                                overall = overallType,
                                underlying = underlyingType,
                            )
                        }
                    }
                    is NadelReferencedType.VirtualType -> {
                        val virtualType = engineSchema.typeMap[referencedType.name]!!
                        val backingType = engineSchema.typeMap[referencedType.backingType]!!
                        NadelServiceSchemaElement.VirtualType(
                            service = service,
                            overall = virtualType,
                            underlying = backingType,
                        )
                    }
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
    private fun getReferencedTypes(
        service: Service,
        externalTypes: List<GraphQLNamedType>,
    ): Set<NadelReferencedType> {
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

        // If it can be reached by using your service, you must own it to return it!
        return getReferencedTypeNames(service, definitionNames)
    }

    context(NadelValidationContext)
    private fun isNamespacedOperationType(typeName: String): Boolean {
        return typeName in namespaceTypeNames
    }

    context(NadelValidationContext)
    private fun isOperationType(typeName: String): Boolean {
        return engineSchema.operationTypes.any { it.name == typeName }
    }

    /**
     * @return true to visit
     */
    context(NadelValidationContext)
    private fun visitElement(schemaElement: NadelServiceSchemaElement): Boolean {
        // Returns true if the element was added i.e. haven't visited before
        return visitedTypes.add(schemaElement.toRef())
    }
}
