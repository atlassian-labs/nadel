package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelDeepRenameFieldInstruction
import graphql.nadel.engine.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelRenameFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelDefaultHydrationKey
import graphql.nadel.engine.blueprint.hydration.NadelObjectIdentifierCastingStrategy
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationOperationPlanner.Operation
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLTypeUtil
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Coordinates prepared batch hydrations from one request-scoped root round.
 *
 * The shared path is intentionally narrow: only type-default, object-identified hydrations with
 * compatible execution semantics are pooled. Equal selections deduplicate inputs in one lane;
 * different selections may share one bounded, aliased multi-root backing operation.
 */
internal class NadelBatchHydrationCoordinator(
    private val engine: NextgenEngine,
    private val hydrator: NadelNewBatchHydrator,
    private val planner: NadelBatchHydrationOperationPlanner = NadelBatchHydrationOperationPlanner(),
    private val errorMapper: NadelSharedBatchHydrationErrorMapper = NadelSharedBatchHydrationErrorMapper(),
) {
    private data class MatchObjectIdentifierSignature(
        val sourceIdCast: NadelObjectIdentifierCastingStrategy,
        val resultId: String,
    )

    private data class CompatibilityKey(
        val typeDefaultKey: NadelDefaultHydrationKey,
        val backingService: Service,
        val backingPath: NadelQueryPath,
        val batchArgumentName: String,
        val batchArgumentType: String,
        val batchSize: Int,
        val timeout: Int,
        val matching: List<MatchObjectIdentifierSignature>,
        val nonBatchArguments: Map<String, NormalizedInputValue>,
    )

    private data class ExecutedOperation(
        val operation: Operation,
        val result: ServiceExecutionResult,
    )

    suspend fun hydrate(
        preparedHydrations: List<NadelNewBatchHydrator.PreparedBatchHydration>,
    ): Map<NadelNewBatchHydrator.PreparedBatchHydration, NadelCoalescedBatchHydrationOutput> {
        if (preparedHydrations.size < 2) {
            return hydrateIndividually(preparedHydrations)
        }

        val sharedGroups = preparedHydrations
            .mapIndexedNotNull { stableId, hydration ->
                getEligibleHydration(
                    stableId = stableId,
                    hydration = hydration,
                )
            }
            .groupBy(::getCompatibilityKey)
            .values
            .filter { group -> group.size > 1 }
        val sharedHydrationIds = sharedGroups
            .asSequence()
            .flatten()
            .mapTo(mutableSetOf(), NadelBatchHydrationCoalescingConsumer::stableId)
        val isolatedHydrations = preparedHydrations.filterIndexed { stableId, _ ->
            stableId !in sharedHydrationIds
        }

        return coroutineScope {
            val isolated = async {
                hydrateIndividually(isolatedHydrations)
            }
            val shared = sharedGroups.mapIndexed { groupOrdinal, consumers ->
                async {
                    executeSharedGroup(
                        groupOrdinal = groupOrdinal,
                        consumers = consumers,
                    )
                }
            }

            buildMap {
                putAll(isolated.await())
                shared.awaitAll().forEach(::putAll)
            }
        }
    }

    private suspend fun hydrateIndividually(
        preparedHydrations: List<NadelNewBatchHydrator.PreparedBatchHydration>,
    ): Map<NadelNewBatchHydrator.PreparedBatchHydration, NadelCoalescedBatchHydrationOutput> {
        return coroutineScope {
            preparedHydrations
                .map { hydration ->
                    async {
                        hydration to NadelCoalescedBatchHydrationOutput(
                            instructions = hydrator.hydrate(hydration),
                        )
                    }
                }
                .awaitAll()
                .toMap()
        }
    }

    private fun getEligibleHydration(
        stableId: Int,
        hydration: NadelNewBatchHydrator.PreparedBatchHydration,
    ): NadelBatchHydrationCoalescingConsumer? {
        val context = hydration.context

        // Only initial top-level execution participates in a request round.
        if (context.executionContext.hydrationDetails != null) {
            return null
        }
        if (hasDeferredExecution(context.sourceField)) {
            return null
        }
        if (hasNestedHydrationSelection(hydration)) {
            return null
        }

        val rawInstructions = context.instructionsByObjectTypeNames.values.flatten()
        val instruction = rawInstructions.distinct().singleOrNull()
            ?: return null
        val consumer = NadelBatchHydrationCoalescingConsumer.createOrNull(
            stableId = stableId,
            hydration = hydration,
            instruction = instruction,
        ) ?: return null

        // Error paths can only be attributed while backing and selected paths retain their
        // ordinary shape.
        if (hasResultPathTransform(hydration, instruction)) {
            return null
        }

        val defaultHydrationKeys = rawInstructions.flatMapTo(linkedSetOf()) { candidate ->
            candidate.defaultHydrationKeys
        }
        if (defaultHydrationKeys.size != 1 ||
            rawInstructions.any { candidate -> candidate.defaultHydrationKeys != defaultHydrationKeys }
        ) {
            return null
        }

        val participant = context.executionContext.batchHydrationCoalescingParticipant
            ?: return null
        if (!participant.isEnabledFor(instruction.backingService)) {
            return null
        }

        // Preparation may have selected a different conditional instruction. Preserve that
        // consumer's isolated behavior.
        if (hydration.sourceInputsByInstruction.keys.any { selected ->
                selected !== instruction && selected != instruction
            }
        ) {
            return null
        }

        return consumer
    }

    private fun hasDeferredExecution(field: ExecutableNormalizedField): Boolean {
        return field.deferredExecutions.isNotEmpty() || field.children.any(::hasDeferredExecution)
    }

    private fun hasNestedHydrationSelection(
        hydration: NadelNewBatchHydrator.PreparedBatchHydration,
    ): Boolean {
        val executionBlueprint = hydration.context.executionBlueprint

        fun hasHydrationAtOrBelow(field: ExecutableNormalizedField): Boolean {
            val hasHydration =
                executionBlueprint
                    .getTypeNameToInstructionsMap<NadelHydrationFieldInstruction>(field)
                    .isNotEmpty() ||
                    executionBlueprint
                        .getTypeNameToInstructionsMap<NadelBatchHydrationFieldInstruction>(field)
                        .isNotEmpty()
            return hasHydration || field.children.any(::hasHydrationAtOrBelow)
        }

        return hydration.context.sourceField.children.any(::hasHydrationAtOrBelow)
    }

    private fun hasResultPathTransform(
        hydration: NadelNewBatchHydrator.PreparedBatchHydration,
        instruction: NadelBatchHydrationFieldInstruction,
    ): Boolean {
        val executionBlueprint = hydration.context.executionBlueprint

        fun hasRenameAtOrBelow(field: ExecutableNormalizedField): Boolean {
            val hasRename =
                executionBlueprint
                    .getTypeNameToInstructionMap<NadelRenameFieldInstruction>(field)
                    .isNotEmpty() ||
                    executionBlueprint
                        .getTypeNameToInstructionMap<NadelDeepRenameFieldInstruction>(field)
                        .isNotEmpty()
            return hasRename || field.children.any(::hasRenameAtOrBelow)
        }

        if (hydration.context.sourceField.children.any(::hasRenameAtOrBelow)) {
            return true
        }

        var fieldContainer: GraphQLFieldsContainer = executionBlueprint.engineSchema.queryType
        instruction.queryPathToBackingField.segments.forEachIndexed { index, fieldName ->
            val field = fieldContainer.getFieldDefinition(fieldName)
                ?: return true
            val coordinates = FieldCoordinates.coordinates(fieldContainer, field)
            if (executionBlueprint.fieldInstructions[coordinates]
                    .orEmpty()
                    .any { fieldInstruction ->
                        fieldInstruction is NadelRenameFieldInstruction ||
                            fieldInstruction is NadelDeepRenameFieldInstruction
                    }
            ) {
                return true
            }

            if (index < instruction.queryPathToBackingField.segments.lastIndex) {
                fieldContainer = GraphQLTypeUtil.unwrapAll(field.type) as? GraphQLFieldsContainer
                    ?: return true
            }
        }

        return false
    }

    private fun getCompatibilityKey(
        consumer: NadelBatchHydrationCoalescingConsumer,
    ): CompatibilityKey {
        val instruction = consumer.instruction
        val (batchArgument) = NadelBatchHydrationInputBuilder.getBatchInputDef(instruction)
            ?: error("Batch hydration must have one batch input")
        val nonBatchArguments = NadelBatchHydrationInputBuilder
            .getNonBatchInputValues(instruction, consumer.context.sourceField)
            .mapKeys { (argument) -> argument.name }

        return CompatibilityKey(
            typeDefaultKey = instruction.defaultHydrationKeys.single(),
            backingService = instruction.backingService,
            backingPath = instruction.queryPathToBackingField,
            batchArgumentName = batchArgument.name,
            batchArgumentType = GraphQLTypeUtil.simplePrint(batchArgument.backingArgumentDef.type),
            batchSize = instruction.batchSize,
            timeout = instruction.timeout,
            matching = consumer.objectIdentifiers.map { objectId ->
                MatchObjectIdentifierSignature(
                    sourceIdCast = objectId.sourceIdCast,
                    resultId = objectId.resultId,
                )
            },
            nonBatchArguments = nonBatchArguments,
        )
    }

    private suspend fun executeSharedGroup(
        groupOrdinal: Int,
        consumers: List<NadelBatchHydrationCoalescingConsumer>,
    ): Map<NadelNewBatchHydrator.PreparedBatchHydration, NadelCoalescedBatchHydrationOutput> {
        val plan = planner.plan(
            groupOrdinal = groupOrdinal,
            consumers = consumers,
        ) ?: return hydrateIndividually(consumers.map { consumer -> consumer.hydration })

        val executedOperations = coroutineScope {
            plan.operations.map { operation ->
                async {
                    val representative = operation.consumers.first()
                    val hydrationDetails = makeHydrationDetails(representative)
                        .withConsumers(
                            operation.consumers.map { consumer ->
                                makeHydrationDetails(consumer).toConsumerDetails()
                            },
                        )
                    ExecutedOperation(
                        operation = operation,
                        result = engine.executeHydration(
                            topLevelFields = operation.queries.map { query -> query.field },
                            service = representative.instruction.backingService,
                            executionContext = representative.context.executionContext,
                            hydrationDetails = hydrationDetails,
                        ),
                    )
                }
            }.awaitAll()
        }

        // Errors are attributed before the indexer removes artificial identifier fields.
        val errorOutputsByStableId = LinkedHashMap<Int, NadelCoalescedBatchHydrationOutput>()
        executedOperations.forEach { executed ->
            errorMapper.getOutputs(
                result = executed.result,
                operation = executed.operation,
            ).forEach { (stableId, output) ->
                errorOutputsByStableId[stableId] =
                    errorOutputsByStableId.getOrDefault(
                        stableId,
                        NadelCoalescedBatchHydrationOutput.EMPTY,
                    ) + output
            }
        }

        val sharedIndexesByLane = plan.lanes.associateWith { lane ->
            val resolvedBatches = executedOperations.flatMap { executed ->
                executed.operation.queries
                    .filter { query -> query.lane === lane }
                    .map { query ->
                        NadelSharedResolvedObjectBatch(
                            result = executed.result,
                            resultPath = query.resultPath,
                        )
                    }
            }
            if (resolvedBatches.isEmpty()) {
                emptyMap()
            } else {
                val representative = lane.consumers.first()
                hydrator.indexSharedResults(
                    instruction = representative.instruction,
                    aliasHelper = lane.aliasHelper,
                    objectIdentifiers = lane.objectIdentifiers,
                    batches = resolvedBatches,
                )
            }
        }

        return buildMap {
            plan.lanes.forEach { lane ->
                lane.consumers.forEach { consumer ->
                    val dataOutput = NadelCoalescedBatchHydrationOutput(
                        instructions = hydrator.materializeSharedResults(
                            hydration = consumer.hydration,
                            instruction = consumer.instruction,
                            indexedResults = sharedIndexesByLane.getValue(lane),
                        ),
                        resultFieldOrders = consumer.sourceObjectsMetadata.map { metadata ->
                            NadelCoalescedResultFieldOrder(
                                parent = metadata.sourceObject,
                                resultKeys = consumer.context.sourceField.parent
                                    ?.children
                                    .orEmpty()
                                    .map { field -> field.resultKey }
                                    .distinct(),
                            )
                        },
                    )
                    put(
                        consumer.hydration,
                        dataOutput + errorOutputsByStableId.getOrDefault(
                            consumer.stableId,
                            NadelCoalescedBatchHydrationOutput.EMPTY,
                        ),
                    )
                }
            }
        }
    }

    private fun makeHydrationDetails(
        consumer: NadelBatchHydrationCoalescingConsumer,
    ): ServiceExecutionHydrationDetails {
        val instruction = consumer.instruction
        val hydrationSourceService = consumer.executionBlueprint
            .getServiceOwning(instruction.location)!!
        val hydrationBackingField =
            FieldCoordinates.coordinates(instruction.backingFieldContainer, instruction.backingFieldDef)

        return ServiceExecutionHydrationDetails(
            instruction = instruction,
            timeout = instruction.timeout,
            batchSize = instruction.batchSize,
            hydrationSourceService = hydrationSourceService,
            hydrationVirtualField = instruction.location,
            hydrationBackingField = hydrationBackingField,
            fieldPath = consumer.context.sourceField.listOfResultKeys,
        )
    }
}
