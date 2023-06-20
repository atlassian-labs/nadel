package graphql.nadel.validation

import graphql.Scalars.GraphQLID
import graphql.Scalars.GraphQLString
import graphql.nadel.Service
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
import graphql.nadel.schema.NadelDirectives.hydratedDirectiveDefinition
import graphql.nadel.validation.NadelSchemaValidationError.DuplicatedUnderlyingType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleFieldOutputType
import graphql.nadel.validation.NadelSchemaValidationError.IncompatibleType
import graphql.nadel.validation.NadelSchemaValidationError.MissingUnderlyingType
import graphql.nadel.validation.util.NadelBuiltInTypes.allNadelBuiltInTypeNames
import graphql.nadel.validation.util.NadelSchemaUtil.getUnderlyingName
import graphql.nadel.validation.util.NadelSchemaUtil.getUnderlyingType
import graphql.nadel.validation.util.getReachableTypeNames
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLImplementingType
import graphql.schema.GraphQLInterfaceType
import graphql.schema.GraphQLNamedOutputType
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType
import graphql.schema.GraphQLUnionType
import graphql.schema.GraphQLUnmodifiedType

internal class NadelTypeValidation(
    private val context: NadelValidationContext,
    private val overallSchema: GraphQLSchema,
    services: Map<String, Service>,
) {
    private val fieldValidation = NadelFieldValidation(overallSchema, services, this)
    private val inputValidation = NadelInputValidation()
    private val unionValidation = NadelUnionValidation(this)
    private val enumValidation = NadelEnumValidation()
    private val interfaceValidation = NadelInterfaceValidation()
    private val namespaceValidation = NadelNamespaceValidation(overallSchema)

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
            unionValidation.validate(schemaElement) +
            interfaceValidation.validate(schemaElement) +
            namespaceValidation.validate(schemaElement) +
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

        return false
    }

    private fun getServiceTypes(
        service: Service,
    ): Pair<List<NadelServiceSchemaElement>, List<NadelSchemaValidationError>> {
        val errors = mutableListOf<NadelSchemaValidationError>()
        val hydrationUnions = getHydrationUnions(service)
        val namesUsed = getTypeNamesUsed(service, externalTypes = hydrationUnions)

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

    private fun getHydrationUnions(service: Service): Set<GraphQLUnionType> {
        return service.definitionRegistry
            .definitions
            .asSequence()
            .filterIsInstance<GraphQLUnionType>()
            .filter { union ->
                overallSchema.typeMap
                    .values
                    .asSequence()
                    .filterIsInstance<GraphQLFieldsContainer>()
                    .flatMap {
                        it.fieldDefinitions
                    }
                    .filter {
                        it.type.unwrapAll().name == union.name
                    }
                    .all {
                        it.hasAppliedDirective(hydratedDirectiveDefinition.name)
                    }
            }
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
            .filterIsInstance<AnyNamedNode>()
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
            .toSet() - namesToIgnore

        // If it can be reached by using your service, you must own it to return it!
        val referencedTypes = getReachableTypeNames(overallSchema, service, definitionNames)

        return (definitionNames + referencedTypes).toSet()
    }

    private fun isNamespacedOperationType(typeName: String): Boolean {
        return namespaceValidation.isNamespacedOperationType(typeName)
    }

    private fun isOperationType(typeName: String): Boolean {
        return overallSchema.operationTypes.map { it.name }.contains(typeName)
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
