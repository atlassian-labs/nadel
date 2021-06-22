package graphql.nadel.enginekt.blueprint

import graphql.language.EnumTypeDefinition
import graphql.language.EnumTypeExtensionDefinition
import graphql.language.ImplementingTypeDefinition
import graphql.language.InputObjectTypeExtensionDefinition
import graphql.language.InterfaceTypeExtensionDefinition
import graphql.language.NamedNode
import graphql.language.ObjectTypeExtensionDefinition
import graphql.language.ScalarTypeExtensionDefinition
import graphql.language.SchemaExtensionDefinition
import graphql.language.UnionTypeExtensionDefinition
import graphql.nadel.Service
import graphql.nadel.dsl.EnumTypeDefinitionWithTransformation
import graphql.nadel.dsl.ExtendedFieldDefinition
import graphql.nadel.dsl.FieldMappingDefinition
import graphql.nadel.dsl.InputObjectTypeDefinitionWithTransformation
import graphql.nadel.dsl.InterfaceTypeDefinitionWithTransformation
import graphql.nadel.dsl.ObjectTypeDefinitionWithTransformation
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.FIELD_ARGUMENT
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.OBJECT_FIELD
import graphql.nadel.dsl.TypeMappingDefinition
import graphql.nadel.dsl.UnderlyingServiceHydration
import graphql.nadel.enginekt.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationActorInputDef
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationStrategy
import graphql.nadel.enginekt.transform.query.QueryPath
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.nadel.enginekt.util.getFieldAt
import graphql.nadel.enginekt.util.getFieldsAlong
import graphql.nadel.enginekt.util.isList
import graphql.nadel.enginekt.util.makeFieldCoordinates
import graphql.nadel.enginekt.util.mapFrom
import graphql.nadel.enginekt.util.strictAssociateBy
import graphql.nadel.enginekt.util.unwrapNonNull
import graphql.nadel.schema.NadelDirectives
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLType

internal typealias AnyNamedNode = NamedNode<*>

internal object NadelExecutionBlueprintFactory {
    fun create(overallSchema: GraphQLSchema, services: List<Service>): NadelOverallExecutionBlueprint {
        return Factory(overallSchema, services).make()
    }
}

private class Factory(
    private val overallSchema: GraphQLSchema,
    private val services: List<Service>,
) {
    private val definitionNamesToService: Map<String, Service> = makeDefinitionNamesToService()
    private val coordinatesToService: Map<FieldCoordinates, Service> = makeCoordinatesToService()

    fun make(): NadelOverallExecutionBlueprint {
        val typeRenameInstructions = makeTypeRenameInstructions().strictAssociateBy {
            it.overallName
        }
        val fieldInstructions = makeFieldInstructions().strictAssociateBy {
            it.location
        }

        return NadelOverallExecutionBlueprint(
            schema = overallSchema,
            fieldInstructions = fieldInstructions,
            typeInstructions = typeRenameInstructions,
            underlyingBlueprints = deriveUnderlyingBlueprints(fieldInstructions, typeRenameInstructions),
            coordinatesToService = coordinatesToService
        )
    }

    private fun makeFieldInstructions(): List<NadelFieldInstruction> {
        return overallSchema.typeMap.values
            .asSequence()
            .filterIsInstance<GraphQLObjectType>()
            .flatMap { type ->
                type.fields
                    .asSequence()
                    // Get the field mapping def
                    .mapNotNull { field ->
                        when (val mappingDefinition = getFieldMappingDefinition(field)) {
                            null -> when (val hydration = getUnderlyingServiceHydration(field)) {
                                null -> null
                                else -> makeHydrationFieldInstruction(type, field, hydration)
                            }
                            else -> when (mappingDefinition.inputPath.size) {
                                1 -> makeRenameInstruction(type, field, mappingDefinition)
                                else -> makeDeepRenameFieldInstruction(type, field, mappingDefinition)
                            }
                        }
                    }
            }
            .toList()
    }

    private fun makeDeepRenameFieldInstruction(
        parentType: GraphQLObjectType,
        field: GraphQLFieldDefinition,
        mappingDefinition: FieldMappingDefinition,
    ): NadelFieldInstruction {
        val location = makeFieldCoordinates(parentType, field)

        return NadelDeepRenameFieldInstruction(
            location,
            QueryPath(mappingDefinition.inputPath),
        )
    }

    private fun makeHydrationFieldInstruction(
        hydratedFieldParentType: GraphQLObjectType,
        hydratedFieldDef: GraphQLFieldDefinition,
        hydration: UnderlyingServiceHydration,
    ): NadelFieldInstruction {
        val hydrationActorService = services.single { it.name == hydration.serviceName }
        val actorFieldSchema = hydrationActorService.underlyingSchema

        val queryPathToActorField = listOfNotNull(hydration.syntheticField, hydration.topLevelField)
        val actorFieldDef = actorFieldSchema.queryType.getFieldAt(queryPathToActorField)!!

        if (hydration.isBatched || /*deprecated*/ actorFieldDef.type.unwrapNonNull().isList) {
            require(actorFieldDef.type.unwrapNonNull().isList) { "Batched hydration at '$queryPathToActorField' requires a list output type" }
            return makeBatchHydrationFieldInstruction(
                parentType = hydratedFieldParentType,
                hydratedFieldDef = hydratedFieldDef,
                actorFieldDef = actorFieldDef,
                hydration = hydration,
                actorService = hydrationActorService,
            )
        }

        val actorInputValueDefs = getHydrationArguments(
            hydration = hydration,
            hydratedFieldParentType = hydratedFieldParentType,
            hydratedFieldDef = hydratedFieldDef,
            actorFieldDef = actorFieldDef,
        )
        return NadelHydrationFieldInstruction(
            location = makeFieldCoordinates(hydratedFieldParentType, hydratedFieldDef),
            hydratedFieldDef = hydratedFieldDef,
            actorService = hydrationActorService,
            queryPathToActorField = QueryPath(queryPathToActorField),
            actorFieldDef = actorFieldDef,
            actorInputValueDefs = actorInputValueDefs,
            hydrationStrategy = getHydrationStrategy(
                hydratedFieldParentType = hydratedFieldParentType,
                hydratedFieldDef = hydratedFieldDef,
                actorFieldDef = actorFieldDef,
                actorInputValueDefs = actorInputValueDefs,
            ),
        )
    }

    private fun getHydrationStrategy(
        hydratedFieldParentType: GraphQLObjectType,
        hydratedFieldDef: GraphQLFieldDefinition,
        actorFieldDef: GraphQLFieldDefinition,
        actorInputValueDefs: List<NadelHydrationActorInputDef>,
    ): NadelHydrationStrategy {
        val manyToOneInputDef = actorInputValueDefs
            .asSequence()
            .mapNotNull { inputValueDef ->
                if (inputValueDef.valueSource !is NadelHydrationActorInputDef.ValueSource.FieldResultValue) {
                    return@mapNotNull null
                }

                val underlyingParentType = getUnderlyingType(hydratedFieldParentType)
                    ?: error("No underlying type for: ${hydratedFieldParentType.name}")
                val fieldDefs = underlyingParentType.getFieldsAlong(inputValueDef.valueSource.queryPathToField.segments)
                inputValueDef.takeIf {
                    fieldDefs.any { fieldDef ->
                        fieldDef.type.unwrapNonNull().isList
                            && !actorFieldDef.getArgument(inputValueDef.name).type.unwrapNonNull().isList
                    }
                }
            }
            .emptyOrSingle()

        return if (manyToOneInputDef != null) {
            if (!hydratedFieldDef.type.unwrapNonNull().isList) {
                error("Illegal hydration declaration")
            }
            NadelHydrationStrategy.ManyToOne(manyToOneInputDef)
        } else {
            NadelHydrationStrategy.OneToOne
        }
    }

    private fun makeBatchHydrationFieldInstruction(
        parentType: GraphQLObjectType,
        hydratedFieldDef: GraphQLFieldDefinition,
        actorFieldDef: GraphQLFieldDefinition,
        hydration: UnderlyingServiceHydration,
        actorService: Service,
    ): NadelFieldInstruction {
        val location = makeFieldCoordinates(parentType, hydratedFieldDef)

        return NadelBatchHydrationFieldInstruction(
            location = location,
            hydratedFieldDef = hydratedFieldDef,
            actorService = actorService,
            queryPathToActorField = QueryPath(listOfNotNull(hydration.syntheticField, hydration.topLevelField)),
            actorFieldDef = actorFieldDef,
            actorInputValueDefs = getHydrationArguments(hydration, parentType, hydratedFieldDef, actorFieldDef),
            batchSize = hydration.batchSize ?: 50,
            batchHydrationMatchStrategy = if (hydration.isObjectMatchByIndex) {
                NadelBatchHydrationMatchStrategy.MatchIndex
            } else {
                NadelBatchHydrationMatchStrategy.MatchObjectIdentifier(objectId = hydration.objectIdentifier)
            },
        )
    }

    private fun makeRenameInstruction(
        parentType: GraphQLObjectType,
        field: GraphQLFieldDefinition,
        mappingDefinition: FieldMappingDefinition,
    ): NadelRenameFieldInstruction {
        return NadelRenameFieldInstruction(
            location = makeFieldCoordinates(parentType, field),
            underlyingName = mappingDefinition.inputPath.single(),
        )
    }

    private fun makeTypeRenameInstructions(): Sequence<NadelTypeRenameInstruction> {
        return overallSchema.typeMap.values
            .asSequence()
            .filterIsInstance<GraphQLDirectiveContainer>()
            .mapNotNull(this::makeTypeRenameInstruction)
    }

    private fun makeTypeRenameInstruction(type: GraphQLDirectiveContainer): NadelTypeRenameInstruction? {
        return when (val def = type.definition) {
            is ObjectTypeDefinitionWithTransformation -> makeTypeRenameInstruction(def.typeMappingDefinition)
            is InterfaceTypeDefinitionWithTransformation -> makeTypeRenameInstruction(def.typeMappingDefinition)
            is InputObjectTypeDefinitionWithTransformation -> makeTypeRenameInstruction(def.typeMappingDefinition)
            is EnumTypeDefinitionWithTransformation -> makeTypeRenameInstruction(def.typeMappingDefinition)
            else -> when (val typeMappingDef = NadelDirectives.createTypeMapping(type)) {
                null -> null
                else -> makeTypeRenameInstruction(typeMappingDef)
            }
        }
    }

    private fun makeTypeRenameInstruction(typeMappingDefinition: TypeMappingDefinition): NadelTypeRenameInstruction {
        return NadelTypeRenameInstruction(
            overallName = typeMappingDefinition.overallName,
            underlyingName = typeMappingDefinition.underlyingName,
        )
    }

    private fun getHydrationArguments(
        hydration: UnderlyingServiceHydration,
        hydratedFieldParentType: GraphQLObjectType,
        hydratedFieldDef: GraphQLFieldDefinition,
        actorFieldDef: GraphQLFieldDefinition,
    ): List<NadelHydrationActorInputDef> {
        return hydration.arguments.map { remoteArgDef ->
            val valueSource = when (val argSourceType = remoteArgDef.remoteArgumentSource.sourceType) {
                FIELD_ARGUMENT -> {
                    val argumentName = remoteArgDef.remoteArgumentSource.name
                    NadelHydrationActorInputDef.ValueSource.ArgumentValue(
                        argumentName = argumentName,
                        argumentDefinition = hydratedFieldDef.getArgument(argumentName)
                            ?: error("No argument '$argumentName' on field ${hydratedFieldParentType.name}.${hydratedFieldDef.name}"),
                    )
                }
                OBJECT_FIELD -> {
                    val pathToField = remoteArgDef.remoteArgumentSource.path
                    NadelHydrationActorInputDef.ValueSource.FieldResultValue(
                        queryPathToField = QueryPath(pathToField),
                        fieldDefinition = getUnderlyingType(hydratedFieldParentType)?.getFieldAt(
                            pathToField)
                            ?: error("No field defined at: ${hydratedFieldParentType.name}.${pathToField.joinToString(".")}"),
                    )
                }
                else -> error("Unsupported remote argument source type: '$argSourceType'")
            }

            NadelHydrationActorInputDef(
                name = remoteArgDef.name,
                actorArgumentDef = actorFieldDef.getArgument(remoteArgDef.name),
                valueSource = valueSource,
            )
        }
    }

    private fun <T : GraphQLType> getUnderlyingType(overallType: T): T? {
        val renameInstruction = makeTypeRenameInstruction(overallType as? GraphQLDirectiveContainer ?: return null)
        val service = definitionNamesToService[overallType.name]
            ?: error("Unknown service for type: ${overallType.name}")
        val underlyingName = renameInstruction?.underlyingName ?: overallType.name
        return service.underlyingSchema.getTypeAs(underlyingName)
    }

    private fun getFieldMappingDefinition(field: GraphQLFieldDefinition): FieldMappingDefinition? {
        val extendedDef = field.definition as? ExtendedFieldDefinition
        return extendedDef?.fieldTransformation?.fieldMappingDefinition
            ?: NadelDirectives.createFieldMapping(field)
    }

    private fun getUnderlyingServiceHydration(field: GraphQLFieldDefinition): UnderlyingServiceHydration? {
        val extendedDef = field.definition as? ExtendedFieldDefinition
        return extendedDef?.fieldTransformation?.underlyingServiceHydration
            ?: NadelDirectives.createUnderlyingServiceHydration(field)
    }

    private fun deriveUnderlyingBlueprints(
        fieldInstructions: Map<FieldCoordinates, NadelFieldInstruction>,
        typeRenameInstructions: Map<String, NadelTypeRenameInstruction>,
    ): Map<String, NadelExecutionBlueprint> {
        val typeInstructionsByServiceName = typeRenameInstructions.values
            .groupBy { instruction ->
                definitionNamesToService[instruction.overallName]!!.name
            }
            .mapValues { (_, typeInstructionsForService) ->
                mapFrom(
                    typeInstructionsForService.map { instruction: NadelTypeRenameInstruction ->
                        // Make sure to key by underlying name
                        instruction.underlyingName to instruction
                    },
                )
            }

        val fieldInstructionsByServiceName = fieldInstructions.values
            .groupBy { instruction ->
                coordinatesToService[instruction.location]!!.name
            }
            .mapValues { (_, fieldInstructionsForService) ->
                mapFrom(
                    fieldInstructionsForService.mapNotNull { instruction: NadelFieldInstruction ->
                        val underlyingTypeName = instruction.location.typeName.let { overallTypeName ->
                            typeRenameInstructions[overallTypeName]?.underlyingName ?: overallTypeName
                        }
                        val underlyingFieldName = when (instruction) {
                            is NadelRenameFieldInstruction -> instruction.underlyingName
                            else -> return@mapNotNull null
                        }
                        val underlyingFieldCoordinates = makeFieldCoordinates(underlyingTypeName, underlyingFieldName)
                        instruction.copy(location = underlyingFieldCoordinates)
                            .let {
                                it.location to it
                            }
                    },
                )
            }

        return mapFrom(
            services.map { service ->
                service.name to NadelUnderlyingExecutionBlueprint(
                    schema = service.underlyingSchema,
                    fieldInstructions = fieldInstructionsByServiceName[service.name] ?: emptyMap(),
                    typeInstructions = typeInstructionsByServiceName[service.name] ?: emptyMap(),
                )
            }
        )
    }

    private fun makeDefinitionNamesToService(): Map<String, Service> {
        return mapFrom(
            services.flatMap { service ->
                val operationTypes = service.definitionRegistry.operationMap.values.flatten()
                service.definitionRegistry.definitions
                    .filterIsInstance<AnyNamedNode>()
                    .filterNot {
                        it is ObjectTypeExtensionDefinition
                            || it is InterfaceTypeExtensionDefinition
                            || it is EnumTypeExtensionDefinition
                            || it is ScalarTypeExtensionDefinition
                            || it is InputObjectTypeExtensionDefinition
                            || it is SchemaExtensionDefinition
                            || it is UnionTypeExtensionDefinition
                    }
                    .filterNot { def -> def in operationTypes }
                    .map { def -> def.name to service }
            }
        )
    }

    private fun makeCoordinatesToService(): Map<FieldCoordinates, Service> {
        return mapFrom(
            services.flatMap { service ->
                service.definitionRegistry.definitions
                    .filterIsInstance<AnyNamedNode>()
                    .flatMap { typeDef ->
                        when (typeDef) {
                            is EnumTypeDefinition -> typeDef.enumValueDefinitions.map { enumValue ->
                                makeFieldCoordinates(typeDef.name, enumValue.name)
                            }
                            is ImplementingTypeDefinition -> typeDef.fieldDefinitions.map { fieldDef ->
                                makeFieldCoordinates(typeDef.name, fieldDef.name)
                            }
                            else -> emptyList()
                        }
                    }
                    .map { coordinates -> coordinates to service }
            }
        )
    }
}
