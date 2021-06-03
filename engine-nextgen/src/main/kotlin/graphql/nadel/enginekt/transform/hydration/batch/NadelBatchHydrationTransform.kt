package graphql.nadel.enginekt.transform.hydration.batch

import graphql.introspection.Introspection.TypeNameMetaFieldDef
import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.blueprint.getInstructionsOfTypeForField
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.enginekt.transform.NadelTransformFieldResult
import graphql.nadel.enginekt.transform.NadelTransformUtil
import graphql.nadel.enginekt.transform.hydration.NadelHydrationFieldsBuilder
import graphql.nadel.enginekt.transform.hydration.NadelHydrationUtil.makeTypeNameField
import graphql.nadel.enginekt.transform.hydration.batch.NadelBatchHydrationTransform.State
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.toBuilder
import graphql.normalized.NormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLSchema
import graphql.schema.FieldCoordinates.coordinates as makeFieldCoordinates

internal class NadelBatchHydrationTransform(
    engine: NextgenEngine,
) : NadelTransform<State> {
    private val logic = Logic()
    private val hydrator = NadelBatchHydrator(engine, logic)

    data class State(
        val instructions: Map<FieldCoordinates, NadelBatchHydrationFieldInstruction>,
        val executionContext: NadelExecutionContext,
        val field: NormalizedField,
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
        val instructions = executionBlueprint.fieldInstructions
            .getInstructionsOfTypeForField<NadelBatchHydrationFieldInstruction>(field)

        return if (instructions.isNotEmpty()) {
            return State(
                instructions,
                executionContext,
                field,
                alias = "kt_batch_hydration",
            )
        } else {
            null
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
                NadelHydrationFieldsBuilder.getArtificialFields(service, executionPlan, fieldCoordinates, instruction)
                    .map {
                        it.toBuilder().alias(getArtificialFieldResultKey(state, it)).build()
                    }
            } + makeTypeNameField(state),
        )
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

        return hydrator.hydrate(state, executionPlan, parentNodes)
    }

    private fun makeTypeNameField(state: State): NormalizedField {
        return makeTypeNameField(
            alias = getTypeNameResultKey(state),
            objectTypeNames = state.instructions.keys.map { it.typeName },
        )
    }

    private fun getTypeNameResultKey(state: State): String {
        return TypeNameMetaFieldDef.name + "__" + state.alias
    }

    private fun getArtificialFieldResultKey(state: State, field: NormalizedField): String {
        return getArtificialFieldResultKey(state, fieldName = field.name)
    }

    private fun getArtificialFieldResultKey(state: State, fieldName: String): String {
        return state.alias + "__" + fieldName
    }

    private inner class Logic : NadelBatchHydrationLogic {
        override fun mapFieldPathToResultKeys(state: State, path: List<String>): List<String> {
            return path.mapIndexed { index, segment ->
                when (index) {
                    0 -> getArtificialFieldResultKey(state, segment)
                    else -> segment
                }
            }
        }

        /**
         * Note: this can be null if the type condition was not met
         */
        override fun getMatchingInstruction(
            parentNode: JsonNode,
            state: State,
            executionPlan: NadelExecutionPlan,
        ): NadelBatchHydrationFieldInstruction? {
            val overallTypeName = NadelTransformUtil.getOverallTypename(
                executionPlan = executionPlan,
                node = parentNode,
                typeNameResultKey = getTypeNameResultKey(state),
            )
            return state.instructions[makeFieldCoordinates(overallTypeName, state.field.name)]
        }
    }
}
