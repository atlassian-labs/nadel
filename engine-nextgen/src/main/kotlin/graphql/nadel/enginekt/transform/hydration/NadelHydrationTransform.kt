package graphql.nadel.enginekt.transform.hydration

import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.getInstructionsOfTypeForField
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.enginekt.transform.NadelTransformFieldResult
import graphql.nadel.enginekt.transform.NadelTransformUtil.makeTypeNameField
import graphql.nadel.enginekt.transform.artificial.AliasHelper
import graphql.nadel.enginekt.transform.getInstructionForNode
import graphql.nadel.enginekt.transform.hydration.NadelHydrationTransform.State
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.nadel.enginekt.util.queryPath
import graphql.normalized.NormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLSchema

internal class NadelHydrationTransform(
    private val engine: NextgenEngine,
) : NadelTransform<State> {
    data class State(
        /**
         * The hydration instructions for the [field]. There can be multiple instructions
         * as a [NormalizedField] can have multiple [NormalizedField.objectTypeNames].
         *
         * The [Map.Entry.key] of [FieldCoordinates] denotes a specific object type and
         * its associated instruction.
         */
        val instructions: Map<FieldCoordinates, NadelHydrationFieldInstruction>,
        /**
         * The field in question for the transform, stored for quick access when
         * the [State] is passed around.
         */
        val field: NormalizedField,
        val aliasHelper: AliasHelper,
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: NormalizedField,
    ): State? {
        val hydrationInstructions = executionBlueprint.fieldInstructions
            .getInstructionsOfTypeForField<NadelHydrationFieldInstruction>(overallField)

        return if (hydrationInstructions.isEmpty()) {
            null
        } else {
            State(
                hydrationInstructions,
                overallField,
                aliasHelper = AliasHelper.forField(overallField),
            )
        }
    }

    override suspend fun transformField(
        executionContext: NadelExecutionContext,
        transformer: NadelQueryTransformer.Continuation,
        service: Service,
        overallSchema: GraphQLSchema,
        executionPlan: NadelExecutionPlan,
        field: NormalizedField,
        state: State,
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult(
            newField = null,
            artificialFields = state.instructions.flatMap { (fieldCoordinates, instruction) ->
                NadelHydrationFieldsBuilder.getArtificialFields(
                    service = service,
                    executionPlan = executionPlan,
                    aliasHelper = state.aliasHelper,
                    fieldCoordinates = fieldCoordinates,
                    instruction = instruction,
                )
            } + makeTypeNameField(state),
        )
    }

    private fun makeTypeNameField(
        state: State,
    ): NormalizedField {
        return makeTypeNameField(
            aliasHelper = state.aliasHelper,
            objectTypeNames = state.instructions.keys.map { it.typeName },
        )
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        overallSchema: GraphQLSchema,
        executionPlan: NadelExecutionPlan,
        service: Service,
        overallField: NormalizedField,
        underlyingParentField: NormalizedField,
        result: ServiceExecutionResult,
        state: State,
    ): List<NadelResultInstruction> {
        val parentNodes = JsonNodeExtractor.getNodesAt(
            data = result.data,
            queryPath = underlyingParentField.queryPath,
            flatten = true,
        )

        return parentNodes.flatMap {
            hydrate(
                parentNode = it,
                state = state,
                executionPlan = executionPlan,
                hydrationField = overallField,
                executionContext = executionContext,
            )
        }
    }

    private suspend fun hydrate(
        parentNode: JsonNode,
        state: State,
        executionPlan: NadelExecutionPlan,
        hydrationField: NormalizedField, // Field asking for hydration from the overall query
        executionContext: NadelExecutionContext,
    ): List<NadelResultInstruction> {
        val instruction = state.instructions.getInstructionForNode(
            executionPlan = executionPlan,
            aliasHelper = state.aliasHelper,
            parentNode = parentNode,
        )

        // Do nothing if there is no hydration instruction associated with this result
        instruction ?: return emptyList()

        val result = engine.executeHydration(
            service = instruction.actorService,
            topLevelField = NadelHydrationFieldsBuilder.getActorQuery(
                instruction = instruction,
                aliasHelper = state.aliasHelper,
                hydrationField = hydrationField,
                parentNode = parentNode,
            ),
            pathToSourceField = instruction.queryPathToActorField,
            executionContext = executionContext,
        )

        val data = JsonNodeExtractor.getNodesAt(
            data = result.data,
            queryPath = instruction.queryPathToActorField,
        ).emptyOrSingle()

        return listOf(
            NadelResultInstruction.Set(
                subjectPath = parentNode.resultPath + hydrationField.resultKey,
                newValue = data?.value,
            ),
        )
    }
}
