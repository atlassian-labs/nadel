package graphql.nadel.enginekt.transform.hydration

import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelEngineExecutionHooks
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.blueprint.getTypeNameToInstructionsMap
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationStrategy
import graphql.nadel.enginekt.transform.GraphQLObjectTypeName
import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.enginekt.transform.NadelTransformFieldResult
import graphql.nadel.enginekt.transform.NadelTransformUtil.makeTypeNameField
import graphql.nadel.enginekt.transform.artificial.NadelAliasHelper
import graphql.nadel.enginekt.transform.getInstructionsForNode
import graphql.nadel.enginekt.transform.hydration.NadelHydrationTransform.State
import graphql.nadel.enginekt.transform.hydration.NadelHydrationUtil.getInstructionsToAddErrors
import graphql.nadel.enginekt.transform.query.NadelQueryPath
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.transform.result.json.JsonNodes
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.nadel.enginekt.util.queryPath
import graphql.nadel.enginekt.util.toBuilder
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.FieldCoordinates
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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
                    NadelHydrationFieldsBuilder.makeFieldsUsedAsActorInputValues(
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

        val jobs: List<Deferred<List<NadelResultInstruction>>> = coroutineScope {
            parentNodes.map {
                async {
                    hydrate(
                        parentNode = it,
                        state = state,
                        executionBlueprint = executionBlueprint,
                        fieldToHydrate = overallField,
                        executionContext = executionContext,
                    )
                }
            }
        }

        return jobs.awaitAll().flatten()
    }

    private suspend fun hydrate(
        parentNode: JsonNode,
        state: State,
        executionBlueprint: NadelOverallExecutionBlueprint,
        fieldToHydrate: ExecutableNormalizedField, // Field asking for hydration from the overall query
        executionContext: NadelExecutionContext,
    ): List<NadelResultInstruction> {
        val instructions = state.instructionsByObjectTypeNames.getInstructionsForNode(
            executionBlueprint = executionBlueprint,
            service = state.hydratedFieldService,
            aliasHelper = state.aliasHelper,
            parentNode = parentNode,
        )

        // Do nothing if there is no hydration instruction associated with this result
        if (instructions.isEmpty()) {
            return emptyList()
        }

        val instruction = getHydrationFieldInstruction(state.aliasHelper, instructions, executionContext.hooks, parentNode)
            ?: return listOf(NadelResultInstruction.Set(parentNode.resultPath + state.hydratedField.fieldName, null))

        val actorQueryResults = coroutineScope {
            NadelHydrationFieldsBuilder.makeActorQueries(
                instruction = instruction,
                aliasHelper = state.aliasHelper,
                fieldToHydrate = fieldToHydrate,
                parentNode = parentNode,
            ).map { actorQuery ->
                async {
                    val hydrationSourceService = executionBlueprint.getServiceOwning(instruction.location)
                    engine.executeHydration(
                        service = instruction.actorService,
                        topLevelField = actorQuery,
                        pathToActorField = instruction.queryPathToActorField,
                        executionContext = executionContext,
                        serviceHydrationDetails = ServiceExecutionHydrationDetails(
                            instruction.timeout,
                            1,
                            hydrationSourceService,
                            instruction.location
                        )
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

                return listOf(
                    NadelResultInstruction.Set(
                        subjectPath = parentNode.resultPath + fieldToHydrate.resultKey,
                        newValue = data?.value,
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

                return listOf(
                    NadelResultInstruction.Set(
                        subjectPath = parentNode.resultPath + fieldToHydrate.resultKey,
                        newValue = data,
                    ),
                ) + addErrors
            }
        }
    }

    private fun getHydrationFieldInstruction(
        aliasHelper: NadelAliasHelper,
        instructions: List<NadelHydrationFieldInstruction>,
        hooks: ServiceExecutionHooks,
        parentNode: JsonNode,
    ): NadelHydrationFieldInstruction? {
        return when (instructions.size) {
            1 -> instructions.single()
            else -> {
                if (hooks is NadelEngineExecutionHooks) {
                    hooks.getHydrationInstruction(instructions, parentNode, aliasHelper)
                } else {
                    error(
                        "Cannot decide which hydration instruction should be used. Provided ServiceExecutionHooks has " +
                            "to be of type NadelEngineExecutionHooks"
                    )
                }
            }
        }
    }
}
