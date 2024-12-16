package graphql.nadel.validation

import graphql.language.EnumTypeDefinition
import graphql.language.ImplementingTypeDefinition
import graphql.nadel.NadelExecutableService
import graphql.nadel.NadelFieldInstructions
import graphql.nadel.NadelFieldMap
import graphql.nadel.NadelSchemas
import graphql.nadel.Service
import graphql.nadel.definition.hydration.hasHydratedDefinition
import graphql.nadel.definition.hydration.hasIdHydratedDefinition
import graphql.nadel.engine.blueprint.NadelExecutionBlueprint
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.blueprint.NadelTypeRenameInstructions
import graphql.nadel.engine.blueprint.NadelUnderlyingExecutionBlueprint
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.engine.util.toMapStrictly
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.schema.NadelDirectives
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLUnionType

class NadelSchemaValidation internal constructor(
    private val fieldValidation: NadelFieldValidation,
    private val inputValidation: NadelInputValidation,
    private val unionValidation: NadelUnionValidation,
    private val enumValidation: NadelEnumValidation,
    private val interfaceValidation: NadelInterfaceValidation,
    private val namespaceValidation: NadelNamespaceValidation,
    private val virtualTypeValidation: NadelVirtualTypeValidation,
    private val definitionParser: NadelDefinitionParser,
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

        val definitions = definitionParser.parse(engineSchema)
            .onError { return it }

        val context = NadelValidationContext(
            engineSchema = engineSchema,
            servicesByName = servicesByName,
            fieldContributor = fieldContributorMap,
            hydrationUnions = getHydrationUnions(engineSchema),
            namespaceTypeNames = namespaceTypes,
            combinedTypeNames = namespaceTypes + operationTypes.map { it.name },
            definitions = definitions,
            hook = hook,
        )

        val typeValidation = NadelTypeValidation(
            fieldValidation = fieldValidation,
            inputValidation = inputValidation,
            unionValidation = unionValidation,
            enumValidation = enumValidation,
            interfaceValidation = interfaceValidation,
            namespaceValidation = namespaceValidation,
            virtualTypeValidation = virtualTypeValidation,
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
    ): Pair<NadelExecutionBlueprint, NadelOverallExecutionBlueprint> {
        val fieldContributorMap = makeFieldContributorMap(schemas.services)
        val all = validateAll(
            schemas = schemas,
            fieldContributorMap = fieldContributorMap,
        ).asSequence().toList()

        return createExecutionBlueprint(all, schemas, fieldContributorMap) to
            createLegacyExecutionBlueprint(all, schemas, fieldContributorMap)
    }

    private fun createExecutionBlueprint(
        all: List<NadelSchemaValidationResult>,
        schemas: NadelSchemas,
        fieldContributorMap: Map<FieldCoordinates, Service>,
    ): NadelExecutionBlueprint {
        val executableServices = makeExecutableServices(all)

        val fieldOwnershipMap: NadelFieldMap<NadelExecutableService> = NadelFieldMap.from(
            values = fieldContributorMap
                .entries
                .map { (coordinates, service) ->
                    coordinates to executableServices.first { it.name == service.name }
                },
            getCoordinates = { (coords) -> coords },
            getValue = { (_, service) -> service },
        )

        return NadelExecutionBlueprint(
            engineSchema = schemas.engineSchema,
            services = executableServices,
            fieldOwnershipMap = fieldOwnershipMap,
        )
    }

    private fun makeExecutableServices(
        all: List<NadelSchemaValidationResult>,
    ): List<NadelExecutableService> {
        val resultsByService = all
            .asSequence()
            .filterIsInstance<NadelServiceValidationResult>()
            .groupBy {
                it.service
            }

        return resultsByService
            .map { (service, results) ->
                val declaredTypes = results
                    .asSequence()
                    .filterIsInstance<NadelDeclaredServiceTypesResult>()
                    .single()
                val fieldInstructions = results
                    .asSequence()
                    .filterIsInstance<NadelValidatedFieldResult>()
                    .map { it.fieldInstruction }
                    .toList()
                val typeInstructions = results
                    .asSequence()
                    .filterIsInstance<NadelValidatedTypeResult>()
                    .map { it.typeRenameInstruction }
                    .toList()

                NadelExecutableService(
                    name = service.name,
                    fieldInstructions = NadelFieldInstructions(fieldInstructions),
                    typeInstructions = NadelTypeRenameInstructions(typeInstructions),
                    declaredOverallTypeNames = declaredTypes.overallTypeNames,
                    declaredUnderlyingTypeNames = declaredTypes.underlyingTypeNames,
                    serviceExecution = service.serviceExecution,
                    service = service,
                )
            }
    }

    private fun createLegacyExecutionBlueprint(
        all: List<NadelSchemaValidationResult>,
        schemas: NadelSchemas,
        fieldContributorMap: Map<FieldCoordinates, Service>,
    ): NadelOverallExecutionBlueprint {
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
            .filterIsInstance<NadelDeclaredServiceTypesResult>()

        return NadelOverallExecutionBlueprint(
            engineSchema = schemas.engineSchema,
            fieldInstructions = fieldInstructions
                .groupBy(keySelector = { it.fieldInstruction.location }, valueTransform = { it.fieldInstruction }),
            underlyingTypeNamesByService = typenamesForService.associate { it.service.name to it.underlyingTypeNames },
            overallTypeNamesByService = typenamesForService.associate { it.service.name to it.overallTypeNames },
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
