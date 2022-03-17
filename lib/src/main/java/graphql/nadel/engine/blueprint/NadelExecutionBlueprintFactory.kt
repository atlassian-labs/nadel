package graphql.nadel.engine.blueprint

import graphql.nadel.Service
import graphql.nadel.dsl.FieldMappingDefinition
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.FieldArgument
import graphql.nadel.dsl.RemoteArgumentSource.SourceType.ObjectField
import graphql.nadel.dsl.UnderlyingServiceHydration
import graphql.nadel.engine.blueprint.hydration.NadelBatchHydrationMatchStrategy
import graphql.nadel.engine.blueprint.hydration.NadelHydrationActorInputDef
import graphql.nadel.engine.blueprint.hydration.NadelHydrationActorInputDef.ValueSource.FieldResultValue
import graphql.nadel.engine.blueprint.hydration.NadelHydrationStrategy
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.getFieldAt
import graphql.nadel.engine.util.getFieldsAlong
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.makeFieldCoordinates
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.schema.NadelDirectives
import graphql.nadel.util.mapToSet
import graphql.nadel.validation.NadelServiceSchemaElement
import graphql.nadel.validation.util.getServiceTypes
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema

internal object NadelExecutionBlueprintFactory {
    fun create(
        engineSchema: GraphQLSchema,
        services: List<Service>,
        service: Service,
    ): NadelServiceBlueprint {
        return Factory(engineSchema, services).make(service)
    }
}

private class Factory(
    private val engineSchema: GraphQLSchema,
    private val services: List<Service>,
) {
    fun make(service: Service): NadelServiceBlueprint {
        val (types, errors) = getServiceTypes(engineSchema, service)

        // todo: one day we can enable this…
        // Should never happen, schema should have passed validation before being passed to Nadel
        // if (errors.isNotEmpty()) {
        //     throw IllegalStateException("Service types are not valid")
        // }

        val (typeInstructions, fieldInstructions) = makeInstructions(types)

        return NadelServiceBlueprint(
            service = service,
            fieldInstructions = fieldInstructions.groupBy { it.location },
            typeRenames = NadelTypeRenameInstructions(typeInstructions),
            overallTypesDefined = types.mapToSet { it.overall.name },
            underlyingTypesDefined = types.mapToSet { it.underlying.name },
        )
    }

    private fun makeInstructions(
        serviceElements: List<NadelServiceSchemaElement>,
    ): Pair<List<NadelTypeRenameInstruction>, List<NadelFieldInstruction>> {
        val typeInstructions = mutableListOf<NadelTypeRenameInstruction>()
        val fieldInstructions = mutableListOf<NadelFieldInstruction>()
        serviceElements.forEach { type ->
            val (a, b) = makeInstructions(type)
            typeInstructions.addAll(a)
            fieldInstructions.addAll(b)
        }
        return typeInstructions to fieldInstructions
    }

    private fun makeInstructions(
        element: NadelServiceSchemaElement,
    ): Pair<List<NadelTypeRenameInstruction>, List<NadelFieldInstruction>> {
        val typeInstructions = listOfNotNull(
            makeTypeRenameInstruction(element),
        )
        val fieldInstructions = makeFieldInstructions(element)
        return typeInstructions to fieldInstructions
    }

    private fun makeFieldInstructions(type: NadelServiceSchemaElement): List<NadelFieldInstruction> {
        // For now instructions only belong on concrete types
        if (type.overall !is GraphQLObjectType) {
            return emptyList()
        }

        return type.overall.fields
            .asSequence()
            // Get the field mapping def
            .flatMap { field ->
                makeFieldInstruction(type, field)
            }
            .toList()
    }

    private fun makeFieldInstruction(
        type: NadelServiceSchemaElement,
        field: GraphQLFieldDefinition,
    ): List<NadelFieldInstruction> {
        val mappingDefinition = getFieldMappingDefinition(field)
        if (mappingDefinition != null) {
            return listOf(
                when (mappingDefinition.inputPath.size) {
                    1 -> makeRenameInstruction(type, field, mappingDefinition)
                    else -> makeDeepRenameFieldInstruction(type, field, mappingDefinition)
                },
            )
        }

        val hydrations = getUnderlyingServiceHydrations(field)
        if (hydrations.isNotEmpty()) {
            return hydrations
                .map {
                    makeHydrationFieldInstruction(type, field, it)
                }
        }

        return emptyList()
    }

    private fun makeHydrationFieldInstruction(
        hydratedFieldParentType: NadelServiceSchemaElement,
        hydratedFieldDef: GraphQLFieldDefinition,
        hydration: UnderlyingServiceHydration,
    ): NadelFieldInstruction {
        val hydrationActorService = services.single { it.name == hydration.serviceName }
        val queryPathToActorField = hydration.pathToActorField
        val actorFieldDef = engineSchema.queryType.getFieldAt(queryPathToActorField)!!

        if (hydration.isBatched || /*deprecated*/ actorFieldDef.type.unwrapNonNull().isList) {
            require(actorFieldDef.type.unwrapNonNull().isList) { "Batched hydration at '$queryPathToActorField' requires a list output type" }
            return makeBatchHydrationFieldInstruction(
                hydratedFieldParentType = hydratedFieldParentType,
                hydratedFieldDef = hydratedFieldDef,
                hydration = hydration,
                actorService = hydrationActorService,
                actorFieldDef = actorFieldDef
            )
        }

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
            actorInputValueDefs = hydrationArgs,
            timeout = hydration.timeout,
            hydrationStrategy = getHydrationStrategy(
                hydratedFieldParentType = hydratedFieldParentType,
                hydratedFieldDef = hydratedFieldDef,
                actorFieldDef = actorFieldDef,
                actorInputValueDefs = hydrationArgs,
            ),
            sourceFields = hydrationArgs.mapNotNull {
                when (it.valueSource) {
                    is NadelHydrationActorInputDef.ValueSource.ArgumentValue -> null
                    is FieldResultValue -> it.valueSource.queryPathToField
                }
            },
        )
    }

    private fun getHydrationStrategy(
        hydratedFieldParentType: NadelServiceSchemaElement,
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

                val underlyingParentType = hydratedFieldParentType.underlying as GraphQLFieldsContainer
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
        hydratedFieldParentType: NadelServiceSchemaElement,
        hydratedFieldDef: GraphQLFieldDefinition,
        actorFieldDef: GraphQLFieldDefinition,
        hydration: UnderlyingServiceHydration,
        actorService: Service,
    ): NadelFieldInstruction {
        val location = makeFieldCoordinates(hydratedFieldParentType, hydratedFieldDef)

        val batchSize = hydration.batchSize
        val hydrationArgs = getHydrationArguments(hydration, hydratedFieldParentType, hydratedFieldDef, actorFieldDef)

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

        return NadelBatchHydrationFieldInstruction(
            location = location,
            hydratedFieldDef = hydratedFieldDef,
            actorService = actorService,
            queryPathToActorField = NadelQueryPath(hydration.pathToActorField),
            actorInputValueDefs = hydrationArgs,
            timeout = hydration.timeout,
            batchSize = batchSize,
            batchHydrationMatchStrategy = matchStrategy,
            sourceFields = Unit.let {
                val paths = (when (matchStrategy) {
                    NadelBatchHydrationMatchStrategy.MatchIndex -> emptyList()
                    is NadelBatchHydrationMatchStrategy.MatchObjectIdentifier -> listOf(matchStrategy.sourceId)
                    is NadelBatchHydrationMatchStrategy.MatchObjectIdentifiers -> matchStrategy.objectIds.map { it.sourceId }
                } + hydrationArgs.mapNotNull {
                    when (it.valueSource) {
                        is NadelHydrationActorInputDef.ValueSource.ArgumentValue -> null
                        is FieldResultValue -> it.valueSource.queryPathToField
                    }
                }).toSet()

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
                paths
                    .filter {
                        !prefixes.contains(it.segments + "*")
                    }
            },
            actorFieldDef = actorFieldDef
        )
    }

    private fun makeTypeRenameInstruction(type: NadelServiceSchemaElement): NadelTypeRenameInstruction? {
        return if (type.overall.name != type.underlying.name) {
            NadelTypeRenameInstruction(
                overallName = type.overall.name,
                underlyingName = type.underlying.name,
            )
        } else {
            null
        }
    }

    private fun makeRenameInstruction(
        parentType: NadelServiceSchemaElement,
        field: GraphQLFieldDefinition,
        mappingDefinition: FieldMappingDefinition,
    ): NadelRenameFieldInstruction {
        return NadelRenameFieldInstruction(
            location = makeFieldCoordinates(parentType, field),
            underlyingName = mappingDefinition.inputPath.single(),
        )
    }

    private fun makeDeepRenameFieldInstruction(
        parentType: NadelServiceSchemaElement,
        field: GraphQLFieldDefinition,
        mappingDefinition: FieldMappingDefinition,
    ): NadelFieldInstruction {
        val location = makeFieldCoordinates(parentType, field)

        return NadelDeepRenameFieldInstruction(
            location,
            NadelQueryPath(mappingDefinition.inputPath),
        )
    }

    private fun getHydrationArguments(
        hydration: UnderlyingServiceHydration,
        hydratedFieldParentType: NadelServiceSchemaElement,
        hydratedFieldDef: GraphQLFieldDefinition,
        actorFieldDef: GraphQLFieldDefinition,
    ): List<NadelHydrationActorInputDef> {
        val hydratedFieldCoordinates by lazy {
            makeFieldCoordinates(hydratedFieldParentType, hydratedFieldDef)
        }

        return hydration.arguments.map { remoteArgDef ->
            val valueSource = when (val argSourceType = remoteArgDef.remoteArgumentSource.sourceType) {
                FieldArgument -> {
                    val argumentName = remoteArgDef.remoteArgumentSource.argumentName!!

                    NadelHydrationActorInputDef.ValueSource.ArgumentValue(
                        argumentName = argumentName,
                        argumentDefinition = hydratedFieldDef.getArgument(argumentName)
                            ?: error("No argument '$argumentName' on field $hydratedFieldCoordinates"),
                    )
                }
                ObjectField -> {
                    val pathToField = remoteArgDef.remoteArgumentSource.pathToField!!
                    val underlying = hydratedFieldParentType.underlying as GraphQLFieldsContainer

                    FieldResultValue(
                        queryPathToField = NadelQueryPath(pathToField),
                        fieldDefinition = underlying.getFieldAt(pathToField)
                            ?: error("No field defined at: $pathToField"),
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

    private fun getFieldMappingDefinition(field: GraphQLFieldDefinition): FieldMappingDefinition? {
        return NadelDirectives.createFieldMapping(field)
    }

    private fun getUnderlyingServiceHydrations(field: GraphQLFieldDefinition): List<UnderlyingServiceHydration> {
        return NadelDirectives.createUnderlyingServiceHydration(field, engineSchema)
    }

    private fun makeFieldCoordinates(
        parentType: NadelServiceSchemaElement,
        field: GraphQLFieldDefinition,
    ): FieldCoordinates {
        return makeFieldCoordinates(parentType.overall.name, field.name)
    }
}

