package graphql.nadel.validation.util

import graphql.language.ObjectTypeDefinition
import graphql.nadel.Service
import graphql.nadel.engine.util.AnyNamedNode
import graphql.nadel.engine.util.isExtensionDef
import graphql.nadel.engine.util.operationTypes
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.schema.NadelDirectives
import graphql.nadel.schema.NadelDirectives.hydratedDirectiveDefinition
import graphql.nadel.validation.NadelSchemaValidationError
import graphql.nadel.validation.NadelServiceSchemaElement
import graphql.schema.GraphQLNamedType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLUnionType

fun getServiceTypes(
    engineSchema: GraphQLSchema,
    service: Service,
): Pair<List<NadelServiceSchemaElement>, List<NadelSchemaValidationError>> {
    val errors = mutableListOf<NadelSchemaValidationError>()
    val polymorphicHydrationUnions = getPolymorphicHydrationUnions(engineSchema, service)
    val namesUsed = getTypeNamesUsed(engineSchema, service, externalTypes = polymorphicHydrationUnions)

    fun addMissingUnderlyingTypeError(overallType: GraphQLNamedType) {
        errors.add(NadelSchemaValidationError.MissingUnderlyingType(service, overallType))
    }

    return engineSchema
        .typeMap
        .asSequence()
        .filter { (key) ->
            key in namesUsed
        }
        .filterNot { (key) ->
            key in NadelBuiltInTypes.allNadelBuiltInTypeNames
        }
        .mapNotNull { (_, overallType) ->
            val underlyingType = NadelSchemaUtil.getUnderlyingType(overallType, service)

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
                        NadelSchemaValidationError.DuplicatedUnderlyingType(duplicatedTypes)
                    },
            )
        } to errors
}

private fun getPolymorphicHydrationUnions(
    engineSchema: GraphQLSchema,
    service: Service,
): Set<GraphQLUnionType> {
    return service
        .definitionRegistry
        .definitions
        .asSequence()
        .filterIsInstance<ObjectTypeDefinition>()
        .flatMap { it.fieldDefinitions }
        .filter { it.getDirectives(hydratedDirectiveDefinition.name).size > 1 }
        .map { it.type.unwrapAll() }
        .map { engineSchema.getType(it.name) }
        .filterIsInstance<GraphQLUnionType>()
        .toSet()
}

private fun getTypeNamesUsed(
    engineSchema: GraphQLSchema,
    service: Service,
    externalTypes: Set<GraphQLNamedType>,
): Set<String> {
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
                isNamespacedOperationType(engineSchema, typeName = def.name)
            } else {
                true
            }
        }
        .map {
            it.name
        }
        .toSet() - namesToIgnore

    // If it can be reached by using your service, you must own it to return it!
    val referencedTypes = getReachableTypeNames(engineSchema, service, definitionNames)

    return (definitionNames + referencedTypes).toSet()
}

private fun isNamespacedOperationType(
    engineSchema: GraphQLSchema,
    typeName: String,
): Boolean {
    return engineSchema.operationTypes.any { operationType ->
        operationType.fields.any { field ->
            field.hasAppliedDirective(NadelDirectives.namespacedDirectiveDefinition.name) &&
                field.type.unwrapAll().name == typeName
        }
    }
}
