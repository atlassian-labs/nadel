package graphql.nadel.validation

import graphql.language.EnumTypeDefinition
import graphql.language.ImplementingTypeDefinition
import graphql.nadel.NadelSchemas
import graphql.nadel.Service
import graphql.nadel.definition.hydration.isHydrated
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprintImpl
import graphql.nadel.engine.blueprint.NadelTypeRenameInstructions
import graphql.nadel.engine.blueprint.NadelUnderlyingExecutionBlueprint
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.engine.util.toMapStrictly
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.schema.NadelDirectives
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLNamedSchemaElement
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLUnionType

class NadelSchemaValidation(
    // Why is this a constructor argument…
    private val schemas: NadelSchemas,
) {
    fun validate(): Set<NadelSchemaValidationError> {
        return validateAll()
            .filterIsInstanceTo(LinkedHashSet())
    }

    fun validateAll(): List<NadelSchemaValidationResult> {
        val (engineSchema, services) = schemas

        val servicesByName = services.strictAssociateBy(Service::name)

        val operationTypes = getOperationTypeNames(engineSchema)
        val namespaceTypes = getNamespaceOperationTypes(engineSchema)

        val context = NadelValidationContext(
            engineSchema,
            servicesByName,
            makeFieldContributorMap(services),
            getHydrationUnions(engineSchema),
            namespaceTypes,
            namespaceTypes + operationTypes.map { it.name },
        )
        val typeValidation = NadelTypeValidation()

        return with(context) {
            services
                .flatMap {
                    typeValidation.validate(it)
                }
        }
    }

    private fun getHydrationUnions(engineSchema: GraphQLSchema): Set<String> {
        // For each union type, find all fields that use it as an output type
        val fieldsByUnionOutputTypes = engineSchema.typeMap.values
            .asSequence()
            .filterIsInstance<GraphQLFieldsContainer>()
            .flatMap {
                it.fieldDefinitions
            }
            .filter {
                it.type.unwrapAll() is GraphQLUnionType
            }
            .groupBy {
                it.type.unwrapAll().name
            }

        return engineSchema.typeMap.values
            .asSequence()
            .filterIsInstance<GraphQLUnionType>()
            .filter { union ->
                fieldsByUnionOutputTypes[union.name]?.all(GraphQLFieldDefinition::isHydrated) == true
            }
            .map {
                it.name
            }
            .toSet()
    }

    fun validateAndGenerateBlueprint(): NadelOverallExecutionBlueprint {
        val fieldContributorMap = makeFieldContributorMap(schemas.services)

        val all = validateAll()
        val fieldInstructions = all
            .filterIsInstance<NadelValidatedFieldResult>()
            .groupBy {
                it.fieldInstruction.location
            }
            .values
            .flatMap { instructionsForLocation ->
                // ???
                val keepInstructionsFromThisService = instructionsForLocation.first().service.name
                instructionsForLocation
                    .filter {
                        it.service.name == keepInstructionsFromThisService
                    }
            }

        val typeInstructions = all
            .filterIsInstance<NadelValidatedTypeResult>()
        val typenamesForService = all
            .filterIsInstance<NadelTypenamesForService>()

        return NadelOverallExecutionBlueprintImpl(
            engineSchema = schemas.engineSchema,
            fieldInstructions = fieldInstructions
                .groupBy(keySelector = { it.fieldInstruction.location }, valueTransform = { it.fieldInstruction }),
            underlyingTypeNamesByService = typenamesForService.associate { it.service to it.underlyingTypeNames },
            overallTypeNamesByService = typenamesForService.associate { it.service to it.overallTypeNames },
            underlyingBlueprints = schemas.services.associate { service -> // Blank map to ensure that all services are represented
                service.name to NadelUnderlyingExecutionBlueprint(
                    service = service,
                    schema = service.underlyingSchema,
                    typeInstructions = NadelTypeRenameInstructions(emptyList()),
                )
            } +
                typeInstructions.groupBy(
                    keySelector = {
                        it.service
                    },
                ).mapValues { (service, instructions) ->
                    NadelUnderlyingExecutionBlueprint(
                        service,
                        service.underlyingSchema,
                        NadelTypeRenameInstructions(instructions.map { it.typeRenameInstruction }),
                    )
                }.mapKeys { (service) ->
                    service.name
                },
            coordinatesToService = fieldContributorMap, // todo: stop computing twice
            typeRenamesByOverallTypeName = typeInstructions
                .associate {
                    it.typeRenameInstruction.overallName to it.typeRenameInstruction
                },
        )
    }
}

data class NadelServiceSchemaElement(
    val service: Service,
    val overall: GraphQLNamedSchemaElement,
    val underlying: GraphQLNamedSchemaElement,
) {
    internal fun toRef() = NadelServiceSchemaElementRef(service, overall, underlying)
}

/**
 * This is used to create a version of [NadelServiceSchemaElement] that has a proper
 * [hashCode] definition instead of relying on identity hashCodes.
 */
internal data class NadelServiceSchemaElementRef(
    val service: String,
    val overall: String,
    val underlying: String,
) {
    companion object {
        operator fun invoke(
            service: Service,
            overall: GraphQLNamedSchemaElement,
            underlying: GraphQLNamedSchemaElement,
        ) = NadelServiceSchemaElementRef(service.name, overall.name, underlying.name)
    }
}

private fun makeFieldContributorMap(services: List<Service>): Map<FieldCoordinates, Service> {
    return services
        .asSequence()
        .flatMap { service ->
            service.definitionRegistry.definitions
                .asSequence()
                .flatMap { type ->
                    when (type) {
                        is ImplementingTypeDefinition<*> -> type.fieldDefinitions
                            .map { field ->
                                makeFieldCoordinates(type.name, field.name) to service
                            }
                        is EnumTypeDefinition -> type.enumValueDefinitions
                            .map { enum ->
                                makeFieldCoordinates(type.name, enum.name) to service
                            }
                        else -> emptyList()
                    }
                }
        }
        .toMapStrictly()
}

private fun getOperationTypeNames(engineSchema: GraphQLSchema): List<GraphQLObjectType> {
    return listOfNotNull(
        engineSchema.queryType,
        engineSchema.mutationType,
        engineSchema.subscriptionType,
    )
}

private fun getNamespaceOperationTypes(overallSchema: GraphQLSchema): Set<String> {
    val operationTypes = getOperationTypeNames(overallSchema)

    return operationTypes
        .asSequence()
        .flatMap { type ->
            type.fields
        }
        .filter { field ->
            field.hasAppliedDirective(NadelDirectives.namespacedDirectiveDefinition.name)
        }
        .map { field ->
            field.type.unwrapAll().name
        }
        .toSet()
}
