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
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLUnionType
import graphql.schema.idl.ScalarInfo

class NadelSchemaValidation(
    // Why is this a constructor argument…
    private val schemas: NadelSchemas,
) {
    private val fieldContributorMap = makeFieldContributorMap(schemas.services)

    fun validate(): Set<NadelSchemaValidationError> {
        return validateAll()
            .asSequence()
            .filterIsInstanceTo(LinkedHashSet())
    }

    fun validateAll(): NadelSchemaValidationResult {
        val (engineSchema, services) = schemas

        val servicesByName = services.strictAssociateBy(Service::name)

        val operationTypes = getOperationTypeNames(engineSchema)
        val namespaceTypes = getNamespaceOperationTypes(engineSchema)

        val context = NadelValidationContext(
            engineSchema = engineSchema,
            servicesByName = servicesByName,
            fieldContributor = fieldContributorMap,
            hydrationUnions = getHydrationUnions(engineSchema),
            namespaceTypeNames = namespaceTypes,
            combinedTypeNames = namespaceTypes + operationTypes.map { it.name },
        )
        val typeValidation = NadelTypeValidation()

        return with(context) {
            services
                .map {
                    typeValidation.validate(it)
                }
                .toResult()
        }.also {
            context.wouldHaveVisited
                .removeIf {
                    ScalarInfo.isGraphqlSpecifiedScalar(it.overall)
                }
            require(context.visitedTypes.containsAll(context.wouldHaveVisited)) {
                "wat"
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
        val all = validateAll().asSequence().toList()
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
