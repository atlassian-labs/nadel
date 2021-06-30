package graphql.nadel.enginekt.transform.hydration

import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.blueprint.getInstructionsOfTypeForField
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationStrategy
import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.enginekt.transform.NadelTransformFieldResult
import graphql.nadel.enginekt.transform.NadelTransformUtil.makeTypeNameField
import graphql.nadel.enginekt.transform.artificial.NadelAliasHelper
import graphql.nadel.enginekt.transform.getInstructionForNode
import graphql.nadel.enginekt.transform.hydration.NadelHydrationTransform.State
import graphql.nadel.enginekt.transform.hydration.NadelHydrationUtil.getInstructionsToAddErrors
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.query.NadelQueryPath
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.nadel.enginekt.util.queryPath
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.FieldCoordinates
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
        val instructions: Map<FieldCoordinates, NadelHydrationFieldInstruction>,
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
    ): State? {
        val hydrationInstructions = executionBlueprint.fieldInstructions
            .getInstructionsOfTypeForField<NadelHydrationFieldInstruction>(overallField)

        return if (hydrationInstructions.isEmpty()) {
            null
        } else {
            State(
                instructions = hydrationInstructions,
                hydratedFieldService = service,
                hydratedField = overallField,
                aliasHelper = NadelAliasHelper.forField(tag = "hydration", overallField),
            )
        }
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformer.Continuation,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        state: State,
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult(
            newField = null,
            artificialFields = state.instructions.flatMap { (fieldCoordinates, instruction) ->
                NadelHydrationFieldsBuilder.makeFieldsUsedAsActorInputValues(
                    service = service,
                    executionBlueprint = executionBlueprint,
                    aliasHelper = state.aliasHelper,
                    fieldCoordinates = fieldCoordinates,
                    instruction = instruction,
                )
            } + makeTypeNameField(state),
        )
    }

    private fun makeTypeNameField(
        state: State,
    ): ExecutableNormalizedField {
        return makeTypeNameField(
            aliasHelper = state.aliasHelper,
            objectTypeNames = state.instructions.keys.map { it.typeName },
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
    ): List<NadelResultInstruction> {
        val parentNodes = JsonNodeExtractor.getNodesAt(
            data = result.data,
            queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root,
            flatten = true,
        )

        return parentNodes.flatMap {
            hydrate(
                parentNode = it,
                state = state,
                executionBlueprint = executionBlueprint,
                hydrationField = overallField,
                executionContext = executionContext,
            )
        }
    }

    private suspend fun hydrate(
        parentNode: JsonNode,
        state: State,
        executionBlueprint: NadelOverallExecutionBlueprint,
        hydrationField: ExecutableNormalizedField, // Field asking for hydration from the overall query
        executionContext: NadelExecutionContext,
    ): List<NadelResultInstruction> {
        val instruction = state.instructions.getInstructionForNode(
            executionBlueprint = executionBlueprint,
            service = state.hydratedFieldService,
            aliasHelper = state.aliasHelper,
            parentNode = parentNode,
        )

        // Do nothing if there is no hydration instruction associated with this result
        instruction ?: return emptyList()

        val actorQueryResults = coroutineScope {
            NadelHydrationFieldsBuilder.makeActorQueries(
                instruction = instruction,
                aliasHelper = state.aliasHelper,
                hydratedField = hydrationField,
                parentNode = parentNode,
            ).map { actorQuery ->
                async {
                    engine.executeHydration(
                        service = instruction.actorService,
                        topLevelField = actorQuery,
                        pathToActorField = instruction.queryPathToActorField,
                        executionContext = executionContext,
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
                        subjectPath = parentNode.resultPath + hydrationField.resultKey,
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
                        subjectPath = parentNode.resultPath + hydrationField.resultKey,
                        newValue = data,
                    ),
                ) + addErrors
            }
        }
    }
}
