package graphql.nadel.validation

import graphql.language.EnumTypeDefinition
import graphql.language.ImplementingTypeDefinition
import graphql.nadel.NadelSchemas
import graphql.nadel.Service
import graphql.nadel.definition.hydration.hasHydratedDefinition
import graphql.nadel.definition.hydration.hasIdHydratedDefinition
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.blueprint.NadelTypeRenameInstructions
import graphql.nadel.engine.blueprint.NadelUnderlyingExecutionBlueprint
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.engine.util.toMapStrictly
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.schema.NadelDirectives
import graphql.nadel.util.NadelFieldMap
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLUnionType

class NadelSchemaValidation internal constructor(
    private val typeValidation: NadelTypeValidation,
    private val instructionDefinitionParser: NadelInstructionDefinitionParser,
    private val hook: NadelSchemaValidationHook,
) {
    fun validate(
        schemas: NadelSchemas,
    ): Set<NadelSchemaValidationError> {
        return validateAll(schemas)
            .asSequence()
            .filterIsInstanceTo(LinkedHashSet())
    }

    fun validateAll(
        schemas: NadelSchemas,
        fieldContributorMap: Map<FieldCoordinates, Service> = makeFieldContributorMap(schemas.services),
    ): NadelSchemaValidationResult {
        val (engineSchema, services) = schemas

        val servicesByName = services.strictAssociateBy(Service::name)

        val operationTypes = getOperationTypeNames(engineSchema)
        val namespaceTypes = getNamespaceOperationTypes(engineSchema)
        val hiddenTypeNames = getHiddenTypeNames(engineSchema)

        val instructionDefinitions = instructionDefinitionParser.parse(engineSchema)
            .onError { return it }

        val context = NadelValidationContext(
            engineSchema = engineSchema,
            servicesByName = servicesByName,
            fieldContributor = fieldContributorMap,
            hydrationUnions = getHydrationUnions(engineSchema),
            namespaceTypeNames = namespaceTypes,
            combinedTypeNames = namespaceTypes + operationTypes.map { it.name },
            hiddenTypeNames = hiddenTypeNames,
            instructionDefinitions = instructionDefinitions,
            hook = hook,
        )

        return with(context) {
            services
                .map {
                    typeValidation.validate(it)
                }
                .toResult()
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
                fieldsByUnionOutputTypes[union.name]
                    ?.all { field ->
                        field.hasHydratedDefinition() || field.hasIdHydratedDefinition()
                    } == true
            }
            .map {
                it.name
            }
            .toSet()
    }

    fun validateAndGenerateBlueprint(
        schemas: NadelSchemas,
    ): NadelOverallExecutionBlueprint {
        val fieldContributorMap = makeFieldContributorMap(schemas.services)
        val all = validateAll(
            schemas = schemas,
            fieldContributorMap = fieldContributorMap,
        ).asSequence().toList()

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
            .filterIsInstance<NadelReachableServiceTypesResult>()

        return NadelOverallExecutionBlueprint(
            engineSchema = schemas.engineSchema,
            fieldInstructions = NadelFieldMap.groupBy(
                values = fieldInstructions,
                getCoordinates = {
                    it.fieldInstruction.location
                },
                getValue = {
                    it.fieldInstruction
                },
            ),
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

private fun NadelSchemaValidationResult.asSequence(): Sequence<NadelSchemaValidationResult> {
    return if (this is NadelSchemaValidationResults) {
        results.asSequence()
    } else {
        sequenceOf(this)
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

/**
 * Gets the types that will be removed by the `@hidden` directive.
 *
 * i.e. all fields that reference this type are `@hidden`
 */
private fun getHiddenTypeNames(engineSchema: GraphQLSchema): Set<String> {
    val hiddenTypeNames = HashMap<String, Boolean>(engineSchema.typeMap.size)

    engineSchema
        .typeMap
        .values
        .asSequence()
        .filterIsInstance<GraphQLFieldsContainer>()
        .forEach { type ->
            type.fields.forEach { field ->
                val outputTypeName = field.type.unwrapAll().name

                if (field.hasAppliedDirective(NadelDirectives.hiddenDirectiveDefinition.name)) {
                    val existing = hiddenTypeNames[outputTypeName]
                    if (existing == null) {
                        hiddenTypeNames[outputTypeName] = true
                    }
                } else {
                    hiddenTypeNames[outputTypeName] = false
                }
            }
        }

    return hiddenTypeNames.keys
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
