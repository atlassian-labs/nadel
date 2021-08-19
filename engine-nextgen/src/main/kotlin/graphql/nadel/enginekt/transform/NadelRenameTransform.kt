package graphql.nadel.enginekt.transform

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.blueprint.NadelRenameFieldInstruction
import graphql.nadel.enginekt.blueprint.getInstructionsForField
import graphql.nadel.enginekt.transform.NadelRenameTransform.State
import graphql.nadel.enginekt.transform.artificial.NadelAliasHelper
import graphql.nadel.enginekt.transform.query.NFUtil.createField
import graphql.nadel.enginekt.transform.query.NadelQueryPath
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.nadel.enginekt.util.queryPath
import graphql.nadel.enginekt.util.toBuilder
import graphql.normalized.ExecutableNormalizedField
import graphql.schema.FieldCoordinates

internal class NadelRenameTransform : NadelTransform<State> {
    data class State(
        val instructionsByObjectTypeNames: Map<GraphQLObjectTypeName, NadelRenameFieldInstruction>,
        val objectTypesWithoutRename: List<String>,
        val aliasHelper: NadelAliasHelper,
        val overallField: ExecutableNormalizedField,
        val service: Service,
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        overallField: ExecutableNormalizedField,
    ): State? {
        val renameInstructions = executionBlueprint.fieldInstructions
            .getInstructionsForField<NadelRenameFieldInstruction>(overallField)
        if (renameInstructions.isEmpty()) {
            return null
        }

        val objectsWithoutRename = overallField.objectTypeNames.filterNot {
            it in renameInstructions
        }

        return State(
            renameInstructions,
            objectsWithoutRename,
            NadelAliasHelper.forField(tag = "rename", overallField),
            overallField,
            service,
        )
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
            newField = if (state.objectTypesWithoutRename.isNotEmpty()) {
                field.toBuilder()
                    .clearObjectTypesNames()
                    .objectTypeNames(state.objectTypesWithoutRename)
                    .children(transformer.transform(field.children))
                    .build()
            } else {
                null
            },
            artificialFields = makeRenamedFields(state, transformer, executionBlueprint).let {
                when (val typeNameField = makeTypeNameField(state)) {
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
    ): ExecutableNormalizedField? {
        // No need for typename on top level field
        if (state.overallField.queryPath.size == 1) {
            return null
        }

        return NadelTransformUtil.makeTypeNameField(
            aliasHelper = state.aliasHelper,
            objectTypeNames = state.instructionsByObjectTypeNames.keys.toList(),
        )
    }

    private suspend fun makeRenamedFields(
        state: State,
        transformer: NadelQueryTransformer.Continuation,
        executionBlueprint: NadelOverallExecutionBlueprint,
    ): List<ExecutableNormalizedField> {
        return state.instructionsByObjectTypeNames.map { (typeName, instruction) ->
            makeRenamedField(
                state,
                transformer,
                executionBlueprint,
                state.service,
                state.overallField,
                typeName,
                rename = instruction,
            )
        }
    }

    private suspend fun makeRenamedField(
        state: State,
        transformer: NadelQueryTransformer.Continuation,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        field: ExecutableNormalizedField,
        typeName: GraphQLObjectTypeName,
        rename: NadelRenameFieldInstruction,
    ): ExecutableNormalizedField {
        val underlyingTypeName = executionBlueprint.getUnderlyingTypeName(typeName)
        val underlyingObjectType = service.underlyingSchema.getObjectType(underlyingTypeName)
            ?: error("No underlying object type")
        return state.aliasHelper.toArtificial(
            createField(
                schema = service.underlyingSchema,
                parentType = underlyingObjectType,
                queryPathToField = NadelQueryPath(rename.underlyingName),
                fieldArguments = emptyMap(),
                fieldChildren = transformer.transform(field.children),
            )
        )
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        executionBlueprint: NadelOverallExecutionBlueprint,
        service: Service,
        overallField: ExecutableNormalizedField,
        underlyingParentField: ExecutableNormalizedField?, // Overall field
        result: ServiceExecutionResult,
        state: State,
    ): List<NadelResultInstruction> {
        val parentNodes = JsonNodeExtractor.getNodesAt(
            data = result.data,
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

            NadelResultInstruction.Copy(
                subjectPath = sourceFieldNode.resultPath,
                destinationPath = parentNode.resultPath + overallField.resultKey,
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

