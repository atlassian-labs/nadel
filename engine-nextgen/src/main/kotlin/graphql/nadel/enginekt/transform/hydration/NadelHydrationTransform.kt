package graphql.nadel.enginekt.transform.hydration

import graphql.introspection.Introspection.TypeNameMetaFieldDef
import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.getInstructionsOfTypeForField
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.NadelTransformUtil
import graphql.nadel.enginekt.transform.hydration.NadelHydrationTransform.State
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.query.NadelTransform
import graphql.nadel.enginekt.transform.query.NadelTransformFieldResult
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.nadel.enginekt.util.toBuilder
import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedField.newNormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLSchema
import graphql.schema.FieldCoordinates.coordinates as makeFieldCoordinates

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
        /**
         * Used as a prefix or suffix to field names to ensure that artificial fields added
         * by [NadelHydrationTransform] do NOT overlap with existing field result keys.
         */
        val alias: String,
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        field: NormalizedField,
    ): State? {
        val hydrationInstructions = executionBlueprint.fieldInstructions
            .getInstructionsOfTypeForField<NadelHydrationFieldInstruction>(field)

        return if (hydrationInstructions.isEmpty()) {
            null
        } else {
            State(
                hydrationInstructions,
                field,
                alias = "hydration_uuid",
            )
        }
    }

    override suspend fun transformField(
        transformer: NadelQueryTransformer.Continuation,
        service: Service,
        overallSchema: GraphQLSchema,
        executionPlan: NadelExecutionPlan,
        field: NormalizedField,
        state: State,
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult(
            newField = null,
            extraFields = state.instructions.flatMap { (fieldCoordinates, instruction) ->
                NadelHydrationFieldsBuilder.getExtraFields(service, executionPlan, fieldCoordinates, instruction)
                    .map {
                        it.toBuilder().alias(getArtificialFieldResultKey(state, it)).build()
                    }
            } + makeTypeNameField(state),
        )
    }

    /**
     * Read [State.instructions]
     *
     * In the case that there are multiple [FieldCoordinates] for a single [NormalizedField]
     * we need to know which type we are dealing with, so we use this to add a `__typename`
     * selection to determine the behavior on [getResultInstructions].
     *
     * This detail is omitted from most examples in this file for simplicity.
     */
    private fun makeTypeNameField(
        state: State,
    ): NormalizedField {
        // TODO: DRY this code, this is copied from deep rename
        return newNormalizedField()
            .alias(getTypeNameResultKey(state))
            .fieldName(TypeNameMetaFieldDef.name)
            .objectTypeNames(state.instructions.keys.map { it.typeName })
            .build()
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        overallSchema: GraphQLSchema,
        executionPlan: NadelExecutionPlan,
        service: Service,
        field: NormalizedField,
        result: ServiceExecutionResult,
        state: State,
    ): List<NadelResultInstruction> {
        val parentNodes = JsonNodeExtractor.getNodesAt(
            data = result.data,
            queryResultKeyPath = field.listOfResultKeys.dropLast(1),
            flatten = true,
        )

        return parentNodes.flatMap {
            hydrate(
                parentNode = it,
                state = state,
                executionPlan = executionPlan,
                hydrationField = field,
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
        val instruction = getMatchingInstruction(
            parentNode,
            state,
            executionPlan,
        )

        // Do nothing if there is no hydration instruction associated with this result
        instruction ?: return emptyList()

        val result = engine.executeHydration(
            service = instruction.sourceService,
            topLevelField = NadelHydrationFieldsBuilder.getQuery(
                instruction = instruction,
                hydrationField = hydrationField,
                parentNode = parentNode,
                pathToResultKeys = { path ->
                    mapFieldPathToResultKeys(state, path)
                },
            ),
            pathToSourceField = instruction.pathToSourceField,
            executionContext = executionContext,
        )

        val data = JsonNodeExtractor.getNodesAt(
            data = result.data,
            queryResultKeyPath = instruction.pathToSourceField,
        ).emptyOrSingle()

        return listOf(
            NadelResultInstruction.Set(
                subjectPath = parentNode.path + hydrationField.resultKey,
                newValue = data?.value,
            ),
        )
    }

    private fun mapFieldPathToResultKeys(state: State, path: List<String>): List<String> {
        return path.mapIndexed { index, segment ->
            if (index == 0) {
                getArtificialFieldResultKey(state, segment)
            } else {
                segment
            }
        }
    }

    private fun getArtificialFieldResultKey(state: State, field: NormalizedField): String {
        return getArtificialFieldResultKey(state, fieldName = field.name)
    }

    private fun getArtificialFieldResultKey(state: State, fieldName: String): String {
        return state.alias + "__" + fieldName
    }

    /**
     * Read [State.alias]
     *
     * @return the aliased value of the GraphQL introspection field `__typename`
     */
    private fun getTypeNameResultKey(state: State): String {
        return TypeNameMetaFieldDef.name + "__" + state.alias
    }

    /**
     * Note: this can be null if the type condition was not met
     */
    private fun getMatchingInstruction(
        parentNode: JsonNode,
        state: State,
        executionPlan: NadelExecutionPlan,
    ): NadelHydrationFieldInstruction? {
        val overallTypeName = NadelTransformUtil.getOverallTypename(
            executionPlan = executionPlan,
            node = parentNode,
            typeNameResultKey = getTypeNameResultKey(state),
        )
        return state.instructions[makeFieldCoordinates(overallTypeName, state.field.name)]
    }
}
