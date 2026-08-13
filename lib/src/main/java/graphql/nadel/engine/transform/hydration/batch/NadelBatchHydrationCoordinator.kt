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
import graphql.nadel.engine.transform.result.NadelResultMutation
import graphql.nadel.hooks.NadelBatchHydrationCoalescingKey
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLFieldsContainer
import graphql.schema.GraphQLTypeUtil
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Coordinates all batch hydrations that become ready in one result-transformation wave.
 *
 * Preparation is deliberately performed before deciding between isolated and shared execution.
 * This means source inputs and hook-selected instructions are resolved exactly once and the same
 * [NadelNewBatchHydrator.PreparedBatchHydration] is used by either execution path.
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
        val hookKey: NadelBatchHydrationCoalescingKey,
    )

    private data class EligibleHydration(
        val consumer: NadelBatchHydrationCoalescingConsumer,
        val hookKey: NadelBatchHydrationCoalescingKey,
    )

    private data class ExecutedOperation(
        val operation: Operation,
        val result: ServiceExecutionResult,
    )

    suspend fun hydrate(
        invocations: List<NadelNewBatchHydrator.Invocation>,
    ): Map<Int, List<NadelResultMutation>> {
        val preparedHydrations = invocations.map(hydrator::prepare)
        if (preparedHydrations.size < 2) {
            return hydrateIndividually(preparedHydrations)
        }

        val compatibleGroups = preparedHydrations
            .mapNotNull(::getEligibleHydration)
            .groupBy(::getCompatibilityKey)
            .values
            .filter { group -> group.size > 1 }
            .map { group ->
                group.map(EligibleHydration::consumer)
            }
        val sharedInvocationIds = compatibleGroups
            .asSequence()
            .flatten()
            .mapTo(mutableSetOf()) { consumer -> consumer.invocation.id }
        val isolatedHydrations = preparedHydrations.filter { hydration ->
            hydration.invocation.id !in sharedInvocationIds
        }

        return coroutineScope {
            val isolated = async {
                hydrateIndividually(isolatedHydrations)
            }
            val shared = compatibleGroups.mapIndexed { groupOrdinal, group ->
                async {
                    executeSharedGroup(
                        groupOrdinal = groupOrdinal,
                        consumers = group,
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
    ): Map<Int, List<NadelResultMutation>> {
        return coroutineScope {
            preparedHydrations
                .map { hydration ->
                    hydration.invocation.id to async {
                        hydrator
                            .hydrate(hydration)
                            .map(NadelResultMutation::Instruction)
                    }
                }
                .associate { (id, deferred) ->
                    id to deferred.await()
                }
        }
    }

    private fun getEligibleHydration(
        hydration: NadelNewBatchHydrator.PreparedBatchHydration,
    ): EligibleHydration? {
        val state = hydration.invocation.state

        // A hydration-scoped payload cannot be mapped to one concrete client occurrence, so
        // nested hydration waves retain the isolated behavior.
        if (state.executionContext.hydrationDetails != null) {
            return null
        }

        // Incremental delivery remains isolated. @defer metadata can live on a descendant even
        // when the hydrated field itself is immediate.
        if (hasDeferredExecution(state.virtualField)) {
            return null
        }

        // A nested hydration can append backing errors whose path is relative to its own service
        // call. Until nested executions carry occurrence provenance, the outer shared operation
        // cannot attribute those errors to the correct consumer.
        if (hasNestedHydrationSelection(hydration)) {
            return null
        }

        val rawInstructions = state.instructionsByObjectTypeNames
            .values
            .flatten()
        val instruction = rawInstructions
            .distinct()
            .singleOrNull()
            ?: return null
        val consumer = NadelBatchHydrationCoalescingConsumer.createOrNull(
            hydration = hydration,
            instruction = instruction,
        ) ?: return null

        // Shared error attribution currently understands backing paths in their overall shape.
        // A rename inside the backing path or selected payload changes the service error path
        // before attribution, so retain isolated behavior until query transforms expose a typed
        // error-path mapping.
        if (hasResultPathTransform(hydration, instruction)) {
            return null
        }

        val defaultHydrationKeys = rawInstructions
            .flatMapTo(linkedSetOf()) { candidate ->
                candidate.defaultHydrationKeys
            }
        if (defaultHydrationKeys.size != 1 ||
            rawInstructions.any { candidate ->
                candidate.defaultHydrationKeys != defaultHydrationKeys
            }
        ) {
            return null
        }

        if (!state.executionContext.hints.batchHydrationCoalescing(instruction.backingService)) {
            return null
        }

        // A hook or condition may have selected another instruction while preparing source
        // inputs. Such a consumer must keep the ordinary isolated semantics.
        if (hydration.sourceInputsByInstruction.keys.any { selected ->
                selected !== instruction && selected != instruction
            }
        ) {
            return null
        }

        // Capture the key exactly once per consumer. Null preserves the safe custom-hook
        // fallback; equal non-null keys become one part of the complete compatibility key.
        val hookKey = state.executionContext.hooks.getBatchHydrationCoalescingKey(
            instruction = instruction,
            userContext = state.executionContext.userContext,
        ) ?: return null

        return EligibleHydration(
            consumer = consumer,
            hookKey = hookKey,
        )
    }

    private fun hasDeferredExecution(field: ExecutableNormalizedField): Boolean {
        return field.deferredExecutions.isNotEmpty() ||
            field.children.any(::hasDeferredExecution)
    }

    private fun hasNestedHydrationSelection(
        hydration: NadelNewBatchHydrator.PreparedBatchHydration,
    ): Boolean {
        val executionBlueprint = hydration.invocation.executionBlueprint

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

        return hydration.invocation.state.virtualField.children.any(::hasHydrationAtOrBelow)
    }

    private fun hasResultPathTransform(
        hydration: NadelNewBatchHydrator.PreparedBatchHydration,
        instruction: NadelBatchHydrationFieldInstruction,
    ): Boolean {
        val executionBlueprint = hydration.invocation.executionBlueprint

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

        if (hydration.invocation.state.virtualField.children.any(::hasRenameAtOrBelow)) {
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
        eligibleHydration: EligibleHydration,
    ): CompatibilityKey {
        val consumer = eligibleHydration.consumer
        val instruction = consumer.instruction
        val state = consumer.invocation.state
        val (batchArgument) = NadelBatchHydrationInputBuilder.getBatchInputDef(instruction)
            ?: error("Batch hydration must have one batch input")
        val nonBatchArguments = NadelBatchHydrationInputBuilder
            .getNonBatchInputValues(instruction, state.virtualField)
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
            hookKey = eligibleHydration.hookKey,
        )
    }

    private suspend fun executeSharedGroup(
        groupOrdinal: Int,
        consumers: List<NadelBatchHydrationCoalescingConsumer>,
    ): Map<Int, List<NadelResultMutation>> {
        val plan = planner.plan(
            groupOrdinal = groupOrdinal,
            consumers = consumers,
        ) ?: return hydrateIndividually(consumers.map { consumer -> consumer.hydration })
        val executedOperations = coroutineScope {
            plan.operations
                .map { operation ->
                    async {
                        val operationRepresentative = operation.consumers.first()
                        val hydrationDetails = makeHydrationDetails(operationRepresentative)
                            .withConsumers(
                                operation.consumers.map { consumer ->
                                    makeHydrationDetails(consumer).toConsumerDetails()
                                },
                            )

                        ExecutedOperation(
                            operation = operation,
                            result = engine.executeHydration(
                                topLevelFields = operation.queries.map { query -> query.field },
                                service = operationRepresentative.instruction.backingService,
                                executionContext = operationRepresentative.context.executionContext,
                                hydrationDetails = hydrationDetails,
                            ),
                        )
                    }
                }
                .awaitAll()
        }

        // Attribute errors before indexers remove artificial identifier fields from returned
        // objects.
        val errorMutationsByInvocationId =
            LinkedHashMap<Int, MutableList<NadelResultMutation>>()
        executedOperations.forEach { executed ->
            errorMapper.getMutations(
                result = executed.result,
                operation = executed.operation,
            ).forEach { (invocationId, errorMutations) ->
                errorMutationsByInvocationId
                    .getOrPut(invocationId, ::mutableListOf)
                    .addAll(errorMutations)
            }
        }

        val mutationsByInvocationId = LinkedHashMap<Int, List<NadelResultMutation>>()
        plan.lanes.forEach { lane ->
            val resolvedLaneBatches = executedOperations.flatMap { executed ->
                executed.operation.queries
                    .filter { query -> query.lane === lane }
                    .map { query ->
                        NadelResolvedObjectBatch(
                            sourceInputs = query.batch.sourceInputs,
                            result = executed.result,
                            resultPath = query.resultPath,
                        )
                    }
            }
            val laneIndex = if (resolvedLaneBatches.isEmpty()) {
                emptyMap()
            } else {
                val laneRepresentative = lane.consumers.first()
                hydrator.indexSharedResults(
                    instruction = laneRepresentative.instruction,
                    aliasHelper = lane.aliasHelper,
                    objectIdentifiers = lane.objectIdentifiers,
                    batches = resolvedLaneBatches,
                )
            }

            lane.consumers.forEach { consumer ->
                mutationsByInvocationId[consumer.invocation.id] =
                    hydrator.materializeSharedResults(
                        hydration = consumer.hydration,
                        instruction = consumer.instruction,
                        indexedResults = laneIndex,
                    ).map(NadelResultMutation::Instruction)
            }
        }

        errorMutationsByInvocationId.forEach { (invocationId, errorMutations) ->
            mutationsByInvocationId[invocationId] =
                mutationsByInvocationId.getValue(invocationId) + errorMutations
        }

        return mutationsByInvocationId
    }

    private fun makeHydrationDetails(
        consumer: NadelBatchHydrationCoalescingConsumer,
    ): ServiceExecutionHydrationDetails {
        val instruction = consumer.instruction
        val hydrationSourceService = consumer.context.executionBlueprint
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
