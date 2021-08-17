package graphql.nadel.enginekt.transform.hydration.batch

import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.blueprint.getInstructionsOfTypeForField
import graphql.nadel.enginekt.transform.NadelTransform
import graphql.nadel.enginekt.transform.NadelTransformFieldResult
import graphql.nadel.enginekt.transform.NadelTransformUtil.makeTypeNameField
import graphql.nadel.enginekt.transform.artificial.NadelAliasHelper
import graphql.nadel.enginekt.transform.hydration.NadelHydrationFieldsBuilder
import graphql.nadel.enginekt.transform.hydration.batch.NadelBatchHydrationTransform.State
import graphql.nadel.enginekt.transform.query.NadelQueryPath
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.queryPath
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.FieldCoordinates

internal class NadelBatchHydrationTransform(
    engine: NextgenEngine,
) : NadelTransform<State> {
    private val hydrator = NadelBatchHydrator(engine)

    data class State(
        val executionBlueprint: NadelOverallExecutionBlueprint,
        val instructions: Map<FieldCoordinates, NadelBatchHydrationFieldInstruction>,
        val executionContext: NadelExecutionContext,
        val hydratedField: ExecutableNormalizedField,
        val hydratedFieldService: Service,
        val aliasHelper: NadelAliasHelper,
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
    ): State? {
        val instructions = executionBlueprint.fieldInstructions
            .getInstructionsOfTypeForField<NadelBatchHydrationFieldInstruction>(overallField)

        return if (instructions.isNotEmpty()) {
            return State(
                executionBlueprint = executionBlueprint,
                instructions = instructions,
                executionContext = executionContext,
                hydratedField = overallField,
                hydratedFieldService = service,
                aliasHelper = NadelAliasHelper.forField(tag = "batch_hydration", overallField),
            )
        } else {
            null
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
                    instruction = instruction
                )
            } + makeTypeNameField(state),
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

        return hydrator.hydrate(state, executionBlueprint, parentNodes)
    }

    private fun makeTypeNameField(state: State): ExecutableNormalizedField {
        return makeTypeNameField(
            aliasHelper = state.aliasHelper,
            objectTypeNames = state.instructions.keys.map { it.typeName },
        )
    }
}
