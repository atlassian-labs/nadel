package graphql.nadel.engine.blueprint

import graphql.Scalars.GraphQLBoolean
import graphql.Scalars.GraphQLFloat
import graphql.Scalars.GraphQLID
import graphql.Scalars.GraphQLInt
import graphql.Scalars.GraphQLString
import graphql.language.EnumTypeDefinition
import graphql.language.FieldDefinition
import graphql.language.ImplementingTypeDefinition
import graphql.nadel.Service
import graphql.nadel.dsl.FieldMappingDefinition
import graphql.nadel.dsl.NadelHydrationDefinition
import graphql.nadel.dsl.RemoteArgumentSource
import graphql.nadel.dsl.TypeMappingDefinition
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationActorInputDef
import graphql.nadel.engine.blueprint.hydration.NadelHydrationActorInputDef.ValueSource.FieldResultValue
import graphql.nadel.engine.blueprint.hydration.NadelHydrationCondition
import graphql.nadel.engine.blueprint.hydration.NadelHydrationStrategy
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.util.AnyImplementingTypeDefinition
import graphql.nadel.engine.util.AnyNamedNode
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.getFieldAt
import graphql.nadel.engine.util.getFieldContainerAt
import graphql.nadel.engine.util.getFieldsAlong
import graphql.nadel.engine.util.getOperationType
import graphql.nadel.engine.util.isExtensionDef
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.makeNormalizedInputValue
import graphql.nadel.engine.util.mapFrom
import graphql.nadel.engine.util.strictAssociateBy
import graphql.nadel.engine.util.unwrapAll
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.schema.NadelDirectives
import graphql.nadel.util.AnyAstValue
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLDirectiveContainer
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLSchema
import java.math.BigInteger

internal object NadelExecutionBlueprintFactory {
    fun create(
        engineSchema: GraphQLSchema,
        services: List<Service>,
    ): NadelOverallExecutionBlueprint {
        return Factory(engineSchema, services).make()
    }
}

private class Factory(
    private val engineSchema: GraphQLSchema,
    private val services: List<Service>,
) {
    private val definitionNamesToService: Map<String, Service> = makeDefinitionNamesToService()
    private val coordinatesToService: Map<FieldCoordinates, Service> = makeCoordinatesToService()

    fun make(): NadelOverallExecutionBlueprint {
        val typeRenameInstructions = makeTypeRenameInstructions().strictAssociateBy {
            it.overallName
        }
        val fieldInstructions = makeFieldInstructions().groupBy {
            it.location
        }
        val furtherTypeRenameInstructions = typeRenameInstructions.values +
            SharedTypesAnalysis(engineSchema, services, fieldInstructions, typeRenameInstructions)
                .getTypeRenames()

        val underlyingBlueprints = deriveUnderlyingBlueprints(furtherTypeRenameInstructions)

        // we built this as a map and then only use the keys. In the future we can improve things such
        // that we can use the maps themselves but this is left for another time
        val underlyingTypeNameToOverallNameByService =
            makeUnderlyingTypeNamesToOverallNameByService(services, underlyingBlueprints)
        // the above feeds into the below
        val overAllTypeNameToUnderlyingNameByService =
            makeOverAllTypeNameToUnderlyingNameByService(
                services,
                underlyingBlueprints,
                underlyingTypeNameToOverallNameByService
            )

        val underlyingTypeNamesByService: Map<Service, Set<String>> =
            underlyingTypeNameToOverallNameByService.mapValues { (_, mapOfTypes) ->
                mapOfTypes.keys.toSet()
            }
        val overallTypeNamesByService: Map<Service, Set<String>> =
            overAllTypeNameToUnderlyingNameByService.mapValues { (_, mapOfTypes) ->
                mapOfTypes.keys.toSet()
            }

        return NadelOverallExecutionBlueprint(
            engineSchema = engineSchema,
            fieldInstructions = fieldInstructions,
            underlyingTypeNamesByService = underlyingTypeNamesByService,
            overallTypeNamesByService = overallTypeNamesByService,
            underlyingBlueprints = underlyingBlueprints,
            coordinatesToService = coordinatesToService,
            typeRenamesByOverallTypeName = typeRenameInstructions,
        )
    }

    /**
     * A map per service that contains a map of service underlying type name to overall type name
     */
    private fun makeUnderlyingTypeNamesToOverallNameByService(
        services: List<Service>,
        underlyingBlueprints: Map<String, NadelUnderlyingExecutionBlueprint>,
    ): Map<Service, Map<String, String>> {
        val serviceMap = LinkedHashMap<Service, Map<String, String>>()
        for (service in services) {
            val underlyingNamesToOverallNames: Map<String, String> = service.underlyingSchema
                .typeMap
                .mapValues { (_, underlyingType) ->
                    overAllTypeNameFromUnderlyingType(service, underlyingBlueprints, underlyingType.name)
                }
            serviceMap[service] = underlyingNamesToOverallNames
        }
        return serviceMap
    }

    private fun makeOverAllTypeNameToUnderlyingNameByService(
        services: List<Service>,
        underlyingBlueprints: Map<String, NadelUnderlyingExecutionBlueprint>,
        underlyingTypeNameToOverallNameByService: Map<Service, Map<String, String>>,
    ): Map<Service, Map<String, String>> {
        val serviceMap = LinkedHashMap<Service, Map<String, String>>()
        for (service in services) {
            // this can be !! because we know we built underlyingTypeNameToOverallNameByService per service above
            val underlyingTypeNameToOverallName = underlyingTypeNameToOverallNameByService[service]!!
            val overAllTypeNameToUnderlyingName = LinkedHashMap<String, String>()
            underlyingTypeNameToOverallName.values.forEach { overAllName ->
                val underlyingName = underlyingTypeNameFromOverallType(service, underlyingBlueprints, overAllName)
                overAllTypeNameToUnderlyingName[overAllName] = underlyingName
            }
            serviceMap[service] = overAllTypeNameToUnderlyingName
        }
        return serviceMap
    }

    private fun overAllTypeNameFromUnderlyingType(
        service: Service,
        underlyingBlueprints: Map<String, NadelUnderlyingExecutionBlueprint>,
        underlyingTypeName: String,
    ): String {
        // TODO: THIS SHOULD NOT BE HAPPENING, INTROSPECTIONS ARE DUMB AND DON'T NEED TRANSFORMING
        if (service.name == IntrospectionService.name) {
            return underlyingTypeName
        }

        return underlyingExecutionBlueprint(underlyingBlueprints, service)
            .typeInstructions.getOverallName(underlyingTypeName)
    }

    private fun underlyingTypeNameFromOverallType(
        service: Service,
        underlyingBlueprints: Map<String, NadelUnderlyingExecutionBlueprint>,
        overallTypeName: String,
    ): String {
        // TODO: THIS SHOULD NOT BE HAPPENING, INTROSPECTIONS ARE DUMB AND DON'T NEED TRANSFORMING
        if (service.name == IntrospectionService.name) {
            return overallTypeName
        }

        return underlyingExecutionBlueprint(underlyingBlueprints, service)
            .typeInstructions.getUnderlyingName(overallTypeName)
    }

    private fun underlyingExecutionBlueprint(
        underlyingBlueprints: Map<String, NadelUnderlyingExecutionBlueprint>,
        service: Service,
    ) = (underlyingBlueprints[service.name] ?: error("Could not find service: $service.name"))

    private fun makeFieldInstructions(): List<NadelFieldInstruction> {
        return engineSchema.typeMap.values
            .asSequence()
            .filterIsInstance<GraphQLObjectType>()
            .flatMap { type ->
                type.fields
                    .asSequence()
                    // Get the field mapping def
                    .flatMap { field ->
                        when (val mappingDefinition = getFieldMappingDefinition(field)) {
                            null -> {
                                getUnderlyingServiceHydrations(field)
                                    .map { makeHydrationFieldInstruction(type, field, it) } + makePartitionInstruction(type, field)
                            }
                            else -> when (mappingDefinition.inputPath.size) {
                                1 -> listOf(makeRenameInstruction(type, field, mappingDefinition))
                                else -> listOf(makeDeepRenameFieldInstruction(type, field, mappingDefinition))
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
            NadelQueryPath(mappingDefinition.inputPath),
        )
    }

    private fun makeHydrationFieldInstruction(
        hydratedFieldParentType: GraphQLObjectType,
        hydratedFieldDef: GraphQLFieldDefinition,
        hydration: NadelHydrationDefinition,
    ): NadelFieldInstruction {
        val hydrationActorService = services.single { it.name == hydration.serviceName }
        val queryPathToActorField = hydration.pathToActorField
        val actorFieldDef = engineSchema.queryType.getFieldAt(queryPathToActorField)!!
        val actorFieldContainer = engineSchema.queryType.getFieldContainerAt(queryPathToActorField)!!

        if (hydration.isBatched || /*deprecated*/ actorFieldDef.type.unwrapNonNull().isList) {
            require(actorFieldDef.type.unwrapNonNull().isList) { "Batched hydration at '$queryPathToActorField' requires a list output type" }
            return makeBatchHydrationFieldInstruction(
                parentType = hydratedFieldParentType,
                hydratedFieldDef = hydratedFieldDef,
                hydration = hydration,
                actorService = hydrationActorService,
                actorFieldDef = actorFieldDef,
                actorFieldContainer = actorFieldContainer
            )
        }

        val condition = getHydrationCondition(hydration)

        val hydrationArgs = getHydrationArguments(
            hydration = hydration,
            hydratedFieldParentType = hydratedFieldParentType,
            hydratedFieldDef = hydratedFieldDef,
            actorFieldDef = actorFieldDef,
        )
        return NadelHydrationFieldInstruction(
            location = makeFieldCoordinates(hydratedFieldParentType, hydratedFieldDef),
            hydratedFieldDef = hydratedFieldDef,
            actorService = hydrationActorService,
            queryPathToActorField = NadelQueryPath(queryPathToActorField),
            actorFieldDef = actorFieldDef,
            actorFieldContainer = actorFieldContainer,
            actorInputValueDefs = hydrationArgs,
            timeout = hydration.timeout,
            hydrationStrategy = getHydrationStrategy(
                hydratedFieldParentType = hydratedFieldParentType,
                hydratedFieldDef = hydratedFieldDef,
                actorFieldDef = actorFieldDef,
                actorInputValueDefs = hydrationArgs,
            ),
            sourceFields = getHydrationSourceFields(hydrationArgs, condition),
            condition = condition,
        )
    }

    private fun getHydrationSourceFields(
        hydrationArgs: List<NadelHydrationActorInputDef>,
        condition: NadelHydrationCondition?,
    ): List<NadelQueryPath> {
        val sourceFieldsFromArgs = hydrationArgs.mapNotNull {
            when (it.valueSource) {
                is NadelHydrationActorInputDef.ValueSource.ArgumentValue -> null
                is FieldResultValue -> it.valueSource.queryPathToField
                is NadelHydrationActorInputDef.ValueSource.StaticValue -> null
            }
        }

        if (condition != null) {
            return sourceFieldsFromArgs + condition.fieldPath
        }

        return sourceFieldsFromArgs
    }

    private fun getHydrationCondition(hydration: NadelHydrationDefinition): NadelHydrationCondition? {
        if (hydration.condition == null) {
            return null
        }
        if (hydration.condition.predicate.equals != null) {
            return when (val expectedValue = hydration.condition.predicate.equals) {
                is BigInteger -> NadelHydrationCondition.LongResultEquals(
                    fieldPath = NadelQueryPath(hydration.condition.pathToSourceField),
                    value = expectedValue.longValueExact(),
                )
                is String -> NadelHydrationCondition.StringResultEquals(
                    fieldPath = NadelQueryPath(hydration.condition.pathToSourceField),
                    value = expectedValue
                )
                else -> error("Unexpected type for equals predicate in conditional hydration")
            }
        }
        if (hydration.condition.predicate.startsWith != null) {
            return NadelHydrationCondition.StringResultStartsWith(
                fieldPath = NadelQueryPath(hydration.condition.pathToSourceField),
                prefix = hydration.condition.predicate.startsWith
            )
        }
        if (hydration.condition.predicate.matches != null) {
            return NadelHydrationCondition.StringResultMatches(
                fieldPath = NadelQueryPath(hydration.condition.pathToSourceField),
                regex = hydration.condition.predicate.matches.toRegex()
            )
        }
        error("A conditional hydration is defined but doesnt have any predicate")
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
                if (inputValueDef.valueSource !is FieldResultValue) {
                    return@mapNotNull null
                }

                val underlyingParentType = getUnderlyingType(hydratedFieldParentType, hydratedFieldDef)
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
        actorFieldContainer: GraphQLFieldsContainer,
        hydration: NadelHydrationDefinition,
        actorService: Service,
    ): NadelFieldInstruction {
        val location = makeFieldCoordinates(parentType, hydratedFieldDef)

        val batchSize = hydration.batchSize ?: 50
        val hydrationArgs = getHydrationArguments(hydration, parentType, hydratedFieldDef, actorFieldDef)

        val matchStrategy = if (hydration.isObjectMatchByIndex) {
            NadelBatchHydrationMatchStrategy.MatchIndex
        } else if (hydration.objectIdentifiers?.isNotEmpty() == true) {
            NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers(
                hydration.objectIdentifiers.map { objectIdentifier ->
                    NadelBatchHydrationMatchStrategy.MatchObjectIdentifier(
                        sourceId = NadelQueryPath(
                            objectIdentifier.sourceId.split("."),
                        ),
                        resultId = objectIdentifier.resultId,
                    )
                },
            )
        } else {
            NadelBatchHydrationMatchStrategy.MatchObjectIdentifier(
                sourceId = hydrationArgs
                    .asSequence()
                    .map(NadelHydrationActorInputDef::valueSource)
                    .filterIsInstance<FieldResultValue>()
                    .single()
                    .queryPathToField,
                resultId = hydration.objectIdentifier!!,
            )
        }

        val condition = getHydrationCondition(hydration)

        return NadelBatchHydrationFieldInstruction(
            location = location,
            hydratedFieldDef = hydratedFieldDef,
            actorService = actorService,
            queryPathToActorField = NadelQueryPath(hydration.pathToActorField),
            actorInputValueDefs = hydrationArgs,
            timeout = hydration.timeout,
            batchSize = batchSize,
            batchHydrationMatchStrategy = matchStrategy,
            actorFieldDef = actorFieldDef,
            actorFieldContainer = actorFieldContainer,
            sourceFields = getBatchHydrationSourceFields(matchStrategy, hydrationArgs, condition),
            condition = condition,
        )
    }

    private fun getBatchHydrationSourceFields(
        matchStrategy: NadelBatchHydrationMatchStrategy,
        hydrationArgs: List<NadelHydrationActorInputDef>,
        condition: NadelHydrationCondition?,
    ): List<NadelQueryPath> {
        val paths = (when (matchStrategy) {
            NadelBatchHydrationMatchStrategy.MatchIndex -> emptyList()
            is NadelBatchHydrationMatchStrategy.MatchObjectIdentifier -> listOf(matchStrategy.sourceId)
            is NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers -> matchStrategy.objectIds.map { it.sourceId }
        } + hydrationArgs.flatMap {
            when (val hydrationValueSource: NadelHydrationActorInputDef.ValueSource = it.valueSource) {
                is NadelHydrationActorInputDef.ValueSource.ArgumentValue -> emptyList()
                is FieldResultValue -> selectSourceFieldQueryPaths(hydrationValueSource)
                is NadelHydrationActorInputDef.ValueSource.StaticValue -> emptyList()
            }
        } + listOfNotNull(condition?.fieldPath)).toSet()

        val prefixes = paths
            .asSequence()
            .map {
                it.segments.dropLast(1) + "*"
            }
            .toSet()

        // Say we have paths = [
        //     [page]
        //     [page.id]
        //     [page.status]
        // ]
        // (e.g. page was the input and the page.id and page.status are used to match batch objects)
        // then this maps it to [
        //     [page.id]
        //     [page.status]
        // ]
        val sourceFieldsFromArgs = paths
            .filter {
                !prefixes.contains(it.segments + "*")
            }

        return sourceFieldsFromArgs
    }

    private fun selectSourceFieldQueryPaths(hydrationValueSource: FieldResultValue): List<NadelQueryPath> {
        val hydrationSourceType = hydrationValueSource.fieldDefinition.type.unwrapAll()
        if (hydrationSourceType is GraphQLObjectType) {
            // When the argument of the hydration actor field is an input type and not a primitive
            // we need to add all the input fields to the source fields
            return hydrationSourceType.fields.map { field ->
                hydrationValueSource.queryPathToField.plus(field.name)
            }
        }
        return listOf(hydrationValueSource.queryPathToField)
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

    private fun makePartitionInstruction(
        parentType: GraphQLObjectType,
        field: GraphQLFieldDefinition,
    ): List<NadelPartitionInstruction> {
        val partitionDefinition = NadelDirectives.createPartitionDefinition(field)
            ?: return emptyList()

        return listOf(
            NadelPartitionInstruction(
                location = makeFieldCoordinates(parentType, field),
                pathToPartitionArg = partitionDefinition.pathToPartitionArg,
            )
        )
    }

    private fun makeTypeRenameInstructions(): Sequence<NadelTypeRenameInstruction> {
        return engineSchema.typeMap.values
            .asSequence()
            .filterIsInstance<GraphQLDirectiveContainer>()
            .mapNotNull(this::makeTypeRenameInstruction)
    }

    private fun makeTypeRenameInstruction(type: GraphQLDirectiveContainer): NadelTypeRenameInstruction? {
        return when (type.definition) {
            else -> when (val typeMappingDef = createTypeMapping(type)) {
                null -> null
                else -> makeTypeRenameInstruction(typeMappingDef)
            }
        }
    }

    private fun createTypeMapping(type: GraphQLDirectiveContainer): TypeMappingDefinition? {
        // Fixes bug with jsw schema
        // These don't really mean anything anyway as these cannot be used as fragment type conditions
        // And we don't have variables in normalized queries so they can't be var types
        if (type is GraphQLScalarType) {
            return null
        }

        return NadelDirectives.createTypeMapping(type)
    }

    private fun makeTypeRenameInstruction(typeMappingDefinition: TypeMappingDefinition): NadelTypeRenameInstruction {
        val overallName = typeMappingDefinition.overallName

        return NadelTypeRenameInstruction(
            service = definitionNamesToService[overallName]
                ?: error("Unable to determine what service owns type: $overallName"),
            overallName = overallName,
            underlyingName = typeMappingDefinition.underlyingName,
        )
    }

    private fun getHydrationArguments(
        hydration: NadelHydrationDefinition,
        hydratedFieldParentType: GraphQLObjectType,
        hydratedFieldDef: GraphQLFieldDefinition,
        actorFieldDef: GraphQLFieldDefinition,
    ): List<NadelHydrationActorInputDef> {
        return hydration.arguments.map { remoteArgDef ->
            val valueSource = when (val argSourceType = remoteArgDef.remoteArgumentSource) {
                is RemoteArgumentSource.FieldArgument -> {
                    val argumentName = argSourceType.argumentName
                    val argumentDef = hydratedFieldDef.getArgument(argumentName)
                        ?: error("No argument '$argumentName' on field ${hydratedFieldParentType.name}.${hydratedFieldDef.name}")
                    val defaultValue = if (argumentDef.argumentDefaultValue.isLiteral) {
                        makeNormalizedInputValue(
                            argumentDef.type,
                            argumentDef.argumentDefaultValue.value as AnyAstValue,
                        )
                    } else {
                        null
                    }

                    NadelHydrationActorInputDef.ValueSource.ArgumentValue(
                        argumentName = argSourceType.argumentName,
                        argumentDefinition = argumentDef,
                        defaultValue = defaultValue,
                    )
                }
                is RemoteArgumentSource.ObjectField -> {
                    val pathToField = argSourceType.pathToField
                    FieldResultValue(
                        queryPathToField = NadelQueryPath(pathToField),
                        fieldDefinition = getUnderlyingType(hydratedFieldParentType, hydratedFieldDef)
                            ?.getFieldAt(pathToField)
                            ?: error("No field defined at: ${hydratedFieldParentType.name}.${pathToField.joinToString(".")}"),
                    )
                }
                is RemoteArgumentSource.StaticArgument -> {
                    NadelHydrationActorInputDef.ValueSource.StaticValue(
                        value = argSourceType.staticValue,
                    )
                }
            }

            NadelHydrationActorInputDef(
                name = remoteArgDef.name,
                actorArgumentDef = actorFieldDef.getArgument(remoteArgDef.name),
                valueSource = valueSource,
            )
        }
    }

    /**
     * Gets the underlying type for an [GraphQLObjectType]
     *
     * The [childField] is there in case the [overallType] is an operation type.
     * In that case we still need to know which service's operation type to return.
     */
    private fun getUnderlyingType(
        overallType: GraphQLObjectType,
        childField: GraphQLFieldDefinition,
    ): GraphQLObjectType? {
        val renameInstruction = makeTypeRenameInstruction(overallType as? GraphQLDirectiveContainer ?: return null)
        val underlyingName = renameInstruction?.underlyingName ?: overallType.name

        val fieldCoordinates = makeFieldCoordinates(overallType, childField)

        val service = definitionNamesToService[overallType.name]
            ?: coordinatesToService[fieldCoordinates]
            ?: error("Unable to determine service for $fieldCoordinates")

        return service.underlyingSchema.getTypeAs(underlyingName)
    }

    private fun getFieldMappingDefinition(field: GraphQLFieldDefinition): FieldMappingDefinition? {
        return NadelDirectives.createFieldMapping(field)
    }

    private fun getUnderlyingServiceHydrations(field: GraphQLFieldDefinition): List<NadelHydrationDefinition> {
        return NadelDirectives.createUnderlyingServiceHydration(field, engineSchema)
    }

    private fun deriveUnderlyingBlueprints(
        typeRenameInstructions: List<NadelTypeRenameInstruction>,
    ): Map<String, NadelUnderlyingExecutionBlueprint> {
        val typeInstructionsByServiceName = typeRenameInstructions
            .groupBy { instruction ->
                instruction.service.name
            }

        return mapFrom(
            services.map { service ->
                service.name to NadelUnderlyingExecutionBlueprint(
                    service,
                    schema = service.underlyingSchema,
                    typeInstructions = typeInstructionsByServiceName[service.name] ?: emptyList(),
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
                    .filterNot { it.isExtensionDef }
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

/**
 * So in Nadel we have an problem where our type rename syntax is not sufficient for shared types.
 *
 * For example, given an overall schema:
 *
 * ```graphql
 * service A {
 *   type SharedThing @renamed(from: "Thing") {
 *     id: ID
 *   }
 * }
 * service B {
 *   type Query {
 *     test: SharedThing
 *   }
 * }
 * ```
 *
 * And underlying schemas:
 *
 * ```graphql
 * # Service A's underlying schema
 * type Thing {
 *   id: ID
 * }
 *
 * # Service B's underlying schema
 * type Query {
 *   test: NewThing
 * }
 * type NewThing {
 *   id: ID
 * }
 * ```
 *
 * The question for this schema is: how do we know that `NewThing` from service B is actually
 * `SharedThing` in the overall schema?
 *
 * Ideally we would have:
 *
 * ```graphql
 * type SharedThing @renamed(from: [ {service: "A", type: "Thing"}, {service: "B", type: "NewThing"} ]) {
 *   id: ID
 * }
 * ```
 *
 * But we haven't even fully moved to the new directive based syntax yet. This code bridges that.
 *
 * The code looks at top level fields, and then children fields of the output type, recursively.
 * Then it compares the output type from the overall and underlying schema to build the missing
 * rename definition.
 *
 * e.g. for `test: NewThing` we can see that the overall schema defines it as `test: Shared` so
 * we can assume that `NewThing` was renamed to `Shared` in the overall schema.
 */
private class SharedTypesAnalysis(
    private val engineSchema: GraphQLSchema,
    private val services: List<Service>,
    private val fieldInstructions: Map<FieldCoordinates, List<NadelFieldInstruction>>,
    private val typeRenameInstructions: Map<String, NadelTypeRenameInstruction>,
) {
    companion object {
        private val scalarTypeNames = sequenceOf(GraphQLInt, GraphQLFloat, GraphQLString, GraphQLBoolean, GraphQLID)
            .map { it.name }
            // This is required because of edge cases with central schema ppl turning a Date into a DateTime…
            .plus(sequenceOf("Date", "DateTime"))
            .toSet()
    }

    fun getTypeRenames(): Set<NadelTypeRenameInstruction> {
        return services
            .asSequence()
            .flatMap { service ->
                // Keeps track of visited types to avoid stackoverflow
                val visitedTypes = mutableSetOf<String>()

                val serviceDefinedTypes = service.definitionRegistry.definitions
                    .asSequence()
                    .filterIsInstance<AnyNamedNode>()
                    .filterNot { it.isExtensionDef }
                    .map { it.name }
                    .toSet() + scalarTypeNames

                service.definitionRegistry.operationMap
                    .asSequence()
                    .flatMap forOperation@{ (operationKind, overallOperationTypes) ->
                        val underlyingOperationType = service.underlyingSchema.getOperationType(operationKind)
                            ?: return@forOperation emptySequence()

                        overallOperationTypes
                            .asSequence()
                            .flatMap { overallOperationType ->
                                investigateTypeRenames(
                                    visitedTypes,
                                    service,
                                    serviceDefinedTypes,
                                    overallType = overallOperationType,
                                    underlyingType = underlyingOperationType,
                                    isOperationType = true,
                                )
                            }
                    }
            }
            .toSet()
    }

    private fun investigateTypeRenames(
        visitedTypes: MutableSet<String>,
        service: Service,
        serviceDefinedTypes: Set<String>,
        overallType: AnyImplementingTypeDefinition,
        underlyingType: GraphQLFieldsContainer,
        isOperationType: Boolean = false,
    ): List<NadelTypeRenameInstruction> {
        // Record visited types to avoid stack overflow
        if (!isOperationType && overallType.name in visitedTypes) {
            return emptyList()
        }
        visitedTypes.add(overallType.name)

        return overallType.fieldDefinitions.flatMap { overallField ->
            investigateTypeRenames(
                visitedTypes,
                service,
                serviceDefinedTypes,
                overallField = overallField,
                overallParentType = overallType,
                underlyingParentType = underlyingType,
            )
        }
    }

    private fun investigateTypeRenames(
        visitedTypes: MutableSet<String>,
        service: Service,
        serviceDefinedTypes: Set<String>,
        overallField: FieldDefinition,
        overallParentType: AnyImplementingTypeDefinition,
        underlyingParentType: GraphQLFieldsContainer,
    ): List<NadelTypeRenameInstruction> {
        val underlyingField = getUnderlyingField(overallField, overallParentType, underlyingParentType)
            ?: return emptyList()

        val overallOutputTypeName = overallField.type.unwrapAll().name
        val underlyingOutputTypeName = underlyingField.type.unwrapAll().name
        val outputTypeRenameInstruction = getTypeRenameInstructionOrNull(
            overallTypeName = overallOutputTypeName,
            underlyingTypeName = underlyingOutputTypeName,
            serviceDefinedTypes = serviceDefinedTypes,
            service = service,
        )

        val argumentTypeRenameInstructions = overallField.inputValueDefinitions
            .mapNotNull { overallArgument ->
                val underlyingArgument = underlyingField.getArgument(overallArgument.name)
                getTypeRenameInstructionOrNull(
                    overallTypeName = overallArgument.type.unwrapAll().name,
                    underlyingTypeName = underlyingArgument.type.unwrapAll().name,
                    serviceDefinedTypes = serviceDefinedTypes,
                    service = service,
                )
            }

        val overallOutputTypeDefinition = (engineSchema.getType(overallOutputTypeName) as? GraphQLFieldsContainer?)
            ?.definition as AnyImplementingTypeDefinition?

        return listOfNotNull(outputTypeRenameInstruction) + argumentTypeRenameInstructions + (overallOutputTypeDefinition
            ?.let {
                investigateTypeRenames(
                    visitedTypes,
                    service,
                    serviceDefinedTypes,
                    overallType = overallOutputTypeDefinition,
                    underlyingType = underlyingField.type.unwrapAll() as GraphQLFieldsContainer,
                )
            } ?: emptyList())
    }

    private fun getTypeRenameInstructionOrNull(
        overallTypeName: String,
        underlyingTypeName: String,
        serviceDefinedTypes: Set<String>,
        service: Service,
    ): NadelTypeRenameInstruction? {
        return if (overallTypeName !in serviceDefinedTypes) {
            // Service does not own type, it is shared
            // If the name is different than the overall type, then we mark the rename
            when (underlyingTypeName) {
                overallTypeName -> null
                in scalarTypeNames -> null
                else -> when (typeRenameInstructions[overallTypeName]) {
                    null -> error("Nadel does not allow implicit renames")
                    else -> NadelTypeRenameInstruction(
                        service,
                        overallName = overallTypeName,
                        underlyingName = underlyingTypeName,
                    )
                }
            }
        } else {
            null
        }
    }

    private fun getUnderlyingField(
        overallField: FieldDefinition,
        overallParentType: AnyImplementingTypeDefinition,
        underlyingParentType: GraphQLFieldsContainer,
    ): GraphQLFieldDefinition? {
        // Access instruction via overall schema coordinates
        val overallCoordinates = makeFieldCoordinates(overallParentType.name, overallField.name)

        // Honestly, it would be nice stricter validation here, but it's so cooked that we can't
        val fieldInstructions: List<NadelFieldInstruction> =
            fieldInstructions[overallCoordinates] ?: return underlyingParentType.getField(overallField.name)
        for (instruction in fieldInstructions) {
            if (instruction is NadelRenameFieldInstruction) {
                return underlyingParentType.getField(instruction.underlyingName)
            } else if (instruction is NadelDeepRenameFieldInstruction) {
                return underlyingParentType.getFieldAt(instruction.queryPathToField.segments)
            }
        }
        return null
    }
}
