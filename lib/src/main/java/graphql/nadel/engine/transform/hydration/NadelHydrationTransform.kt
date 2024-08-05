package graphql.nadel.engine.transform.hydration

import graphql.incremental.DeferPayload
import graphql.incremental.DelayedIncrementalPartialResultImpl
import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.blueprint.getTypeNameToInstructionsMap
import graphql.nadel.engine.blueprint.hydration.NadelHydrationStrategy
import graphql.nadel.engine.transform.GraphQLObjectTypeName
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformUtil.makeTypeNameField
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.getInstructionsForNode
import graphql.nadel.engine.transform.hydration.NadelHydrationTransform.State
import graphql.nadel.engine.transform.hydration.NadelHydrationUtil.getInstructionsToAddErrors
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.getFieldDefinitionSequence
import graphql.nadel.engine.util.isList
import graphql.nadel.engine.util.queryPath
import graphql.nadel.engine.util.toBuilder
import graphql.nadel.engine.util.unwrapNonNull
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.FieldCoordinates
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Would recommend a read of [NadelGenericHydrationInstruction] for more context.
 */
internal class NadelHydrationTransform(
    private val engine: NextgenEngine,
) : NadelTransform<State> {
    data class State(
        /**
         * The hydration instructions for the [hydratedField]. There can be multiple instructions
         * as a [ExecutableNormalizedField] can have multiple [ExecutableNormalizedField.objectTypeNames].
         *
         * The [Map.Entry.key] of [FieldCoordinates] denotes a specific object type and
         * its associated instruction.
         */
        val instructionsByObjectTypeNames: Map<GraphQLObjectTypeName, List<NadelHydrationFieldInstruction>>,
        val hydratedFieldService: Service,
        /**
         * The field in question for the transform, stored for quick access when
         * the [State] is passed around.
         */
        val hydratedField: ExecutableNormalizedField,
        val aliasHelper: NadelAliasHelper,
        val executionContext: NadelExecutionContext,
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): State? {
        val hydrationInstructionsByTypeNames = executionBlueprint.fieldInstructions
            .getTypeNameToInstructionsMap<NadelHydrationFieldInstruction>(overallField)

        return if (hydrationInstructionsByTypeNames.isEmpty()) {
            null
        } else {
            State(
                instructionsByObjectTypeNames = hydrationInstructionsByTypeNames,
                hydratedFieldService = service,
                hydratedField = overallField,
                aliasHelper = NadelAliasHelper.forField(tag = "hydration", overallField),
                executionContext = executionContext
            )
        }
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformer,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: State,
    ): NadelTransformFieldResult {
        val objectTypesNoRenames = field.objectTypeNames.filterNot { it in state.instructionsByObjectTypeNames }

        return NadelTransformFieldResult(
            newField = objectTypesNoRenames
                .takeIf(List<GraphQLObjectTypeName>::isNotEmpty)
                ?.let {
                    field.toBuilder()
                        .clearObjectTypesNames()
                        .objectTypeNames(it)
                        .build()
                },
            artificialFields = state.instructionsByObjectTypeNames
                .flatMap { (typeName, instruction) ->
                    NadelHydrationFieldsBuilder.makeRequiredSourceFields(
                        service = service,
                        executionBlueprint = executionBlueprint,
                        aliasHelper = state.aliasHelper,
                        objectTypeName = typeName,
                        instructions = instruction,
                    )
                }
                .let { fields ->
                    when (val typeNameField = makeTypeNameField(state, field)) {
                        null -> fields
                        else -> fields + typeNameField
                    }
                },
        )
    }

    private fun makeTypeNameField(
        state: State,
        field: ExecutableNormalizedField,
    ): ExecutableNormalizedField? {
        val typeNamesWithInstructions = state.instructionsByObjectTypeNames.keys
        val objectTypeNames = field.objectTypeNames
            .filter { it in typeNamesWithInstructions }
            .takeIf { it.isNotEmpty() }
            ?: return null

        return makeTypeNameField(
            aliasHelper = state.aliasHelper,
            objectTypeNames = objectTypeNames,
        )
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        state: State,
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        val parentNodes = nodes.getNodesAt(
            queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root,
            flatten = true,
        )

        return if (isDeferred(executionContext, executionBlueprint, overallField)) {
            deferHydration(parentNodes, state, executionBlueprint, overallField, executionContext)
            return emptyList()
        } else {
            getResultInstructions(parentNodes, state, executionBlueprint, overallField, executionContext)
        }
    }

    private suspend fun getResultInstructions(
        parentNodes: List<JsonNode>,
        state: State,
        executionBlueprint: NadelOverallExecutionBlueprint,
        overallField: ExecutableNormalizedField,
        executionContext: NadelExecutionContext,
    ): List<NadelResultInstruction> {
        return coroutineScope {
            parentNodes
                .map {
                    prepareHydration(
                        parentNode = it,
                        state = state,
                        executionBlueprint = executionBlueprint,
                        fieldToHydrate = overallField,
                        executionContext = executionContext,
                    )
                }
                .map {
                    async {
                        it.hydrate()
                    }
                }
                .awaitAll()
                .flatten()
        }
    }

    private suspend fun deferHydration(
        parentNodes: List<JsonNode>,
        state: State,
        executionBlueprint: NadelOverallExecutionBlueprint,
        overallField: ExecutableNormalizedField,
        executionContext: NadelExecutionContext,
    ) {
        // Prepare the hydrations before we go async
        // We need to do this because if we run it async below, we cannot guarantee that our artificial fields have not yet been removed
        val hydrations = parentNodes.map {
            prepareHydration(
                parentNode = it,
                state = state,
                executionBlueprint = executionBlueprint,
                fieldToHydrate = overallField,
                executionContext = executionContext,
            )
        }

        // This isn't really right… but we start with this
        val label = overallField.deferredExecutions.firstNotNullOfOrNull { it.label }

        executionContext.incrementalResultSupport.defer {
            val instructionSequence = hydrations
                .map {
                    async {
                        it.hydrate()
                    }
                }
                .awaitAll()
                .asSequence()
                .flatten()

            val results = instructionSequence
                .filterIsInstance<NadelResultInstruction.Set>()
                .emptyOrSingle()

            DelayedIncrementalPartialResultImpl.Builder()
                .incrementalItems(
                    listOf(
                        DeferPayload.Builder()
                            .label(label)
                            .data(
                                mapOf(
                                    overallField.resultKey to results?.newValue?.value,
                                ),
                            )
                            .path(
                                overallField.parent?.listOfResultKeys?.let {
                                    @Suppress("USELESS_CAST") // It's not useless because Java (yay)
                                    it as List<Any>
                                } ?: emptyList()
                            )
                            .errors(
                                instructionSequence
                                    .filterIsInstance<NadelResultInstruction.AddError>()
                                    .map {
                                        it.error
                                    }
                                    .toList(),
                            )
                            .build(),
                    ),
                )
                .build()
        }
    }

    private suspend fun prepareHydration(
        parentNode: JsonNode,
        state: State,
        executionBlueprint: NadelOverallExecutionBlueprint,
        fieldToHydrate: ExecutableNormalizedField, // Field asking for hydration from the overall query
        executionContext: NadelExecutionContext,
    ): NadelPreparedHydration {
        val instructions = state.instructionsByObjectTypeNames.getInstructionsForNode(
            executionBlueprint = executionBlueprint,
            service = state.hydratedFieldService,
            aliasHelper = state.aliasHelper,
            parentNode = parentNode,
        )

        // Do nothing if there is no hydration instruction associated with this result
        if (instructions.isEmpty()) {
            return NadelPreparedHydration {
                emptyList()
            }
        }

        val instruction = getHydrationFieldInstruction(state, instructions, executionContext.hooks, parentNode)
            ?: return NadelPreparedHydration {
                listOf(
                    NadelResultInstruction.Set(
                        subject = parentNode,
                        key = NadelResultKey(state.hydratedField.resultKey),
                        newValue = null,
                    ),
                )
            }

        val actorQueries = NadelHydrationFieldsBuilder.makeActorQueries(
            instruction = instruction,
            aliasHelper = state.aliasHelper,
            fieldToHydrate = fieldToHydrate,
            parentNode = parentNode,
            executionBlueprint = executionBlueprint,
        )

        return NadelPreparedHydration {
            val actorQueryResults = coroutineScope {
                actorQueries
                    .map { actorQuery ->
                        async {
                            val hydrationSourceService = executionBlueprint.getServiceOwning(instruction.location)!!
                            val hydrationActorField =
                                FieldCoordinates.coordinates(
                                    instruction.actorFieldContainer,
                                    instruction.actorFieldDef
                                )
                            val serviceHydrationDetails = ServiceExecutionHydrationDetails(
                                timeout = instruction.timeout,
                                batchSize = 1,
                                hydrationSourceService = hydrationSourceService,
                                hydrationSourceField = instruction.location,
                                hydrationActorField = hydrationActorField,
                                fieldPath = fieldToHydrate.listOfResultKeys
                            )
                            engine.executeHydration(
                                service = instruction.actorService,
                                topLevelField = actorQuery,
                                executionContext = executionContext,
                                hydrationDetails = serviceHydrationDetails,
                            )
                        }
                    }.awaitAll()
            }

            when (instruction.hydrationStrategy) {
                is NadelHydrationStrategy.OneToOne -> {
                    // Should not have more than one query for one to one
                    val result = actorQueryResults.emptyOrSingle()

                    val data = result?.data?.let { data ->
                        JsonNodeExtractor.getNodesAt(
                            data = data,
                            queryPath = instruction.queryPathToActorField,
                        ).emptyOrSingle()
                    }

                    val errors = result?.let(::getInstructionsToAddErrors) ?: emptyList()

                    listOf(
                        NadelResultInstruction.Set(
                            subject = parentNode,
                            key = NadelResultKey(fieldToHydrate.resultKey),
                            newValue = JsonNode(data?.value),
                        ),
                    ) + errors
                }
                is NadelHydrationStrategy.ManyToOne -> {
                    val data = actorQueryResults.map { result ->
                        JsonNodeExtractor.getNodesAt(
                            data = result.data,
                            queryPath = instruction.queryPathToActorField,
                        ).emptyOrSingle()?.value
                    }

                    val addErrors = getInstructionsToAddErrors(actorQueryResults)

                    listOf(
                        NadelResultInstruction.Set(
                            subject = parentNode,
                            key = NadelResultKey(fieldToHydrate.resultKey),
                            newValue = JsonNode(data),
                        ),
                    ) + addErrors
                }
            }
        }
    }

    private fun getHydrationFieldInstruction(
        state: State,
        instructions: List<NadelHydrationFieldInstruction>,
        hooks: NadelExecutionHooks,
        parentNode: JsonNode,
    ): NadelHydrationFieldInstruction? {
        if (instructions.any { it.condition == null }) {
            return hooks.getHydrationInstruction(
                instructions,
                parentNode,
                state.aliasHelper,
                state.executionContext.userContext
            )
        }
        return instructions
            .firstOrNull {
                // Note: due to the validation, all instructions in here have a condition, so can call explicitly
                val resultQueryPath = state.aliasHelper.getQueryPath(it.condition!!.fieldPath)
                val node = JsonNodeExtractor.getNodesAt(parentNode, resultQueryPath)
                    .emptyOrSingle()
                it.condition.evaluate(node?.value)
            }
    }

    private fun isDeferred(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        overallField: ExecutableNormalizedField,
    ): Boolean {
        // Disable defer in nested hydration
        if (executionContext.hydrationDetails != null) {
            return false
        }

        return if (executionContext.hints.deferSupport() && overallField.deferredExecutions.isNotEmpty()) {
            // We currently don't support defer if the hydration is inside a List
            return !areAnyParentFieldsOutputtingLists(overallField, executionBlueprint)
        } else {
            false
        }
    }

    private fun areAnyParentFieldsOutputtingLists(
        field: ExecutableNormalizedField,
        executionBlueprint: NadelOverallExecutionBlueprint,
    ): Boolean {
        var cursor: ExecutableNormalizedField? = field.parent

        while (cursor != null) {
            val isList = cursor.getFieldDefinitionSequence(executionBlueprint.engineSchema)
                // todo: I think we don't need to check all of them? just one should be enough since they must conform to the same shape
                .any {
                    it.type.unwrapNonNull().isList
                }

            if (isList) {
                return true
            }

            cursor = cursor.parent
        }

        return false
    }
}

/**
 * A prepared hydration is a hydration that is ready to run, and not dependent on any result objects etc.
 *
 * This exists because hydrations can be `@defer`red.
 *
 * If you defer the hydration code, some values in the result may not be available as they will have been mutated.
 *
 * e.g. for this hydration
 *
 * ``` graphql
 * type Issue {
 *   assignee: User
 *     @hydrated(
 *       service: "users"
 *       field: "user"
 *       arguments: [{name: "id", value: "$source.assigneeId"}]
 *     )
 * }
 * ```
 *
 * Then we need the value of `assigneeId` in the result, but this is an artificial field.
 * Nadel removes artificial fields before the result gets sent back to the caller.
 * So we "prepare" a hydration to ensure we have the value of the artificial field before it gets removed.
 */
private fun interface NadelPreparedHydration {
    suspend fun hydrate(): List<NadelResultInstruction>
}
