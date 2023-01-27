package graphql.nadel.engine.transform

import graphql.nadel.NadelEngineContext
import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.blueprint.NadelRenameFieldInstruction
import graphql.nadel.engine.blueprint.getTypeNameToInstructionMap
import graphql.nadel.engine.transform.NadelRenameTransform.State
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.query.NFUtil.createField
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.query.NadelQueryTransformer
import graphql.nadel.engine.transform.result.NadelResultInstruction
import graphql.nadel.engine.transform.result.NadelResultKey
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.JsonNodeExtractor
import graphql.nadel.engine.transform.result.json.JsonNodes
import graphql.nadel.engine.util.emptyOrSingle
import graphql.nadel.engine.util.queryPath
import graphql.nadel.engine.util.toBuilder
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.FieldCoordinates
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

internal class NadelRenameTransform : NadelTransform<State> {
    data class State(
        val instructionsByObjectTypeNames: Map<GraphQLObjectTypeName, NadelRenameFieldInstruction>,
        val objectTypesWithoutRename: Set<String>,
        val aliasHelper: NadelAliasHelper,
        val overallField: ExecutableNormalizedField,
        val service: Service,
    ) : NadelTransformState

    context(NadelEngineContext, NadelExecutionContext)
    override suspend fun isApplicable(
        service: Service,
        overallField: ExecutableNormalizedField,
        hydrationDetails: ServiceExecutionHydrationDetails?,
    ): State? {
        val renameInstructions = executionBlueprint.fieldInstructions
            .getTypeNameToInstructionMap<NadelRenameFieldInstruction>(overallField)
        if (renameInstructions.isEmpty()) {
            return null
        }

        val objectsWithoutRename = overallField.objectTypeNames
            .asSequence()
            .filterNot { it in renameInstructions }
            .toHashSet()

        return State(
            renameInstructions,
            objectsWithoutRename,
            NadelAliasHelper.forField(tag = "rename", overallField),
            overallField,
            service,
        )
    }

    context(NadelEngineContext, NadelExecutionContext, State)
    override suspend fun transformField(
        transformer: NadelQueryTransformer,
        service: Service,
        field: ExecutableNormalizedField,
        state: State,
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult(
            newField = if (state.objectTypesWithoutRename.isNotEmpty()) {
                field.toBuilder()
                    .clearObjectTypesNames()
                    .objectTypeNames(field.objectTypeNames.filter { it in state.objectTypesWithoutRename })
                    .build()
            } else {
                null
            },
            artificialFields = makeRenamedFields(state, transformer, field, executionBlueprint).let {
                when (val typeNameField = makeTypeNameField(state, field)) {
                    null -> it
                    else -> it + typeNameField
                }
            },
        )
    }

    /**
     * Read [State.instructionsByObjectTypeNames]
     *
     * In the case that there are multiple [FieldCoordinates] for a single [ExecutableNormalizedField]
     * we need to know which type we are dealing with, so we use this to add a `__typename`
     * selection to determine the behavior on [getResultInstructions].
     *
     * This detail is omitted from most examples in this file for simplicity.
     */
    private fun makeTypeNameField(
        state: State,
        field: ExecutableNormalizedField,
    ): ExecutableNormalizedField? {
        // No need for typename on top level field
        if (state.overallField.queryPath.size == 1) {
            return null
        }

        val typeNamesWithInstructions = state.instructionsByObjectTypeNames.keys
        val objectTypeNames = field.objectTypeNames
            .filter { it in typeNamesWithInstructions }
            .takeIf { it.isNotEmpty() }
            ?: return null

        return NadelTransformUtil.makeTypeNameField(
            aliasHelper = state.aliasHelper,
            objectTypeNames = objectTypeNames,
        )
    }

    private suspend fun makeRenamedFields(
        state: State,
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
        executionBlueprint: NadelOverallExecutionBlueprint,
    ): List<ExecutableNormalizedField> {
        val setOfFieldObjectTypeNames = field.objectTypeNames.toSet()
        return state.instructionsByObjectTypeNames
            .asSequence()
            .asFlow() // For coroutines
            .filter { (typeName) ->
                // Don't insert type renames for fields that were never asked for
                typeName in setOfFieldObjectTypeNames
            }
            .map { (typeName, instruction) ->
                makeRenamedField(
                    state,
                    transformer,
                    executionBlueprint,
                    state.service,
                    field,
                    typeName,
                    rename = instruction,
                )
            }
            .toList()
    }

    private suspend fun makeRenamedField(
        state: State,
        transformer: NadelQueryTransformer,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        typeName: GraphQLObjectTypeName,
        rename: NadelRenameFieldInstruction,
    ): ExecutableNormalizedField {
        val underlyingTypeName = executionBlueprint.getUnderlyingTypeName(service, overallTypeName = typeName)
        val underlyingObjectType = service.underlyingSchema.getObjectType(underlyingTypeName)
            ?: error("No underlying object type")
        return state.aliasHelper.toArtificial(
            createField(
                schema = service.underlyingSchema,
                parentType = underlyingObjectType,
                queryPathToField = NadelQueryPath(rename.underlyingName),
                fieldArguments = field.normalizedArguments,
                fieldChildren = transformer.transform(field.children),
            )
        )
    }

    context(NadelEngineContext, NadelExecutionContext, State)
    override suspend fun getResultInstructions(
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?, // Overall field
        result: ServiceExecutionResult,
        state: State,
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        val parentNodes = nodes.getNodesAt(
            queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root,
            flatten = true,
        )

        return parentNodes.mapNotNull instruction@{ parentNode ->
            val instruction = getInstructionForNode(
                state = state,
                executionBlueprint = executionBlueprint,
                parentNode = parentNode,
            ) ?: return@instruction null

            val queryPathForSourceField = NadelQueryPath(state.aliasHelper.getResultKey(instruction.underlyingName))
            val sourceFieldNode = JsonNodeExtractor.getNodesAt(parentNode, queryPathForSourceField)
                .emptyOrSingle() ?: return@instruction null

            NadelResultInstruction.Set(
                subject = parentNode,
                key = NadelResultKey(overallField.resultKey),
                newValue = sourceFieldNode,
            )
        }
    }

    private fun getInstructionForNode(
        state: State,
        executionBlueprint: NadelOverallExecutionBlueprint,
        parentNode: JsonNode,
    ): NadelRenameFieldInstruction? {
        // There can't be multiple instructions for a top level field
        if (state.overallField.queryPath.size == 1) {
            return state.instructionsByObjectTypeNames.values.single()
        }

        return state.instructionsByObjectTypeNames.getInstructionForNode(
            executionBlueprint = executionBlueprint,
            service = state.service,
            aliasHelper = state.aliasHelper,
            parentNode = parentNode,
        )
    }
}

