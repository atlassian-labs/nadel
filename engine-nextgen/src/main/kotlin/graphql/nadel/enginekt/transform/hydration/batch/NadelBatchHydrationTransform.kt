package graphql.nadel.enginekt.transform.hydration.batch

import graphql.introspection.Introspection
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
import graphql.nadel.enginekt.transform.hydration.NadelHydrationFieldsBuilder
import graphql.nadel.enginekt.transform.hydration.NadelHydrationTransform
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

internal class NadelBatchHydrationTransform(
    private val engine: NextgenEngine,
) : NadelTransform<State> {
    data class State(
        val instructions: Map<FieldCoordinates, NadelBatchHydrationFieldInstruction>,
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
            queryResultKeyPath = field.listOfResultKeys.drop(1),
            flatten = true,
        )

        return hydrate(parentNodes)
    }

    private suspend fun hydrate(
        parentNodes: List<JsonNode>,
    ): List<NadelResultInstruction> {
        return emptyList()
    }

    private fun makeTypeNameField(state: State): NormalizedField {
        return makeTypeNameField(
            alias = getTypeNameFieldResultKey(state),
            objectTypeNames = state.instructions.keys.map { it.typeName },
        )
    }

    private fun getTypeNameFieldResultKey(state: State): String {
        return TypeNameMetaFieldDef.name + "__" + state.alias
    }

    private fun getArtificialFieldResultKey(state: State, field: NormalizedField): String {
        return getArtificialFieldResultKey(state, fieldName = field.name)
    }

    private fun getArtificialFieldResultKey(state: State, fieldName: String): String {
        return state.alias + "__" + fieldName
    }
}
