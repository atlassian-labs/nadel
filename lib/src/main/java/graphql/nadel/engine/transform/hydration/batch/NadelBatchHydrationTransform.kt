package graphql.nadel.engine.transform.hydration.batch

import graphql.nadel.NadelEngineContext
import graphql.nadel.NextgenEngine
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.getTypeNameToInstructionsMap
import graphql.nadel.engine.transform.GraphQLObjectTypeName
import graphql.nadel.engine.transform.NadelTransform
import graphql.nadel.engine.transform.NadelTransformFieldResult
import graphql.nadel.engine.transform.NadelTransformState
import graphql.nadel.engine.transform.NadelTransformUtil.makeTypeNameField
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.hydration.NadelHydrationFieldsBuilder
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationTransform.State
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.queryPath
import graphql.nadel.engine.util.toBuilder
import graphql.normalized.ExecutableNormalizedField

internal class NadelBatchHydrationTransform(
    engine: NextgenEngine,
) : NadelTransform<State> {
    private val hydrator = NadelBatchHydrator(engine)

    data class State(
        val instructionsByObjectTypeNames: Map<GraphQLObjectTypeName, List<NadelBatchHydrationFieldInstruction>>,
        val hydratedField: ExecutableNormalizedField,
        val hydratedFieldService: Service,
        val aliasHelper: NadelAliasHelper,
    ) : NadelTransformState

    context(NadelEngineContext, NadelExecutionContext)
    override suspend fun isApplicable(
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): State? {
        val instructionsByObjectTypeName = executionBlueprint.fieldInstructions
            .getTypeNameToInstructionsMap<NadelBatchHydrationFieldInstruction>(overallField)

        return if (instructionsByObjectTypeName.isNotEmpty()) {
            return State(
                instructionsByObjectTypeNames = instructionsByObjectTypeName,
                hydratedField = overallField,
                hydratedFieldService = service,
                aliasHelper = NadelAliasHelper.forField(tag = "batch_hydration", overallField),
            )
        } else {
            null
        }
    }

    context(NadelEngineContext, NadelExecutionContext, State)
    override suspend fun transformField(
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
    ): NadelTransformFieldResult {
        val objectTypesNoRenames = field.objectTypeNames.filterNot { it in instructionsByObjectTypeNames }

        return NadelTransformFieldResult(
            newField = objectTypesNoRenames
                .takeIf(List<GraphQLObjectTypeName>::isNotEmpty)
                ?.let {
                    field.toBuilder()
                        .clearObjectTypesNames()
                        .objectTypeNames(it)
                        .build()
                },
            artificialFields = instructionsByObjectTypeNames
                .flatMap { (objectTypeName, instructions) ->
                    NadelHydrationFieldsBuilder.makeRequiredSourceFields(
                        service = hydratedFieldService,
                        executionBlueprint = executionBlueprint,
                        aliasHelper = aliasHelper,
                        objectTypeName = objectTypeName,
                        instructions = instructions
                    )
                }
                .let { fields ->
                    when (val typeNameField = makeTypeNameField(field)) {
                        null -> fields
                        else -> fields + typeNameField
                    }
                },
        )
    }

    context(NadelEngineContext, NadelExecutionContext, State)
    override suspend fun getResultInstructions(
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?,
        result: ServiceExecutionResult,
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        val parentNodes = nodes.getNodesAt(
            queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root,
            flatten = true,
        )

        return hydrator.hydrate(this@State, executionBlueprint, parentNodes)
    }

    context(State)
    private fun makeTypeNameField(
        field: ExecutableNormalizedField,
    ): ExecutableNormalizedField? {
        val typeNamesWithInstructions = instructionsByObjectTypeNames.keys
        val objectTypeNames = field.objectTypeNames
            .filter { it in typeNamesWithInstructions }
            .takeIf { it.isNotEmpty() }
            ?: return null

        return makeTypeNameField(
            aliasHelper = aliasHelper,
            objectTypeNames = objectTypeNames,
        )
    }
}
