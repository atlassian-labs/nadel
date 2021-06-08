package graphql.nadel.enginekt.transform

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.blueprint.NadelRenameFieldInstruction
import graphql.nadel.enginekt.blueprint.getInstructionsOfTypeForField
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.NadelRenameTransform.State
import graphql.nadel.enginekt.transform.artificial.AliasHelper
import graphql.nadel.enginekt.transform.query.NFUtil.createField
import graphql.nadel.enginekt.transform.query.NadelQueryTransformer
import graphql.nadel.enginekt.transform.query.QueryPath
import graphql.nadel.enginekt.transform.result.NadelResultInstruction
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.nadel.enginekt.util.queryPath
import graphql.nadel.enginekt.util.toBuilder
import graphql.normalized.NormalizedField
import graphql.schema.FieldCoordinates
import graphql.schema.GraphQLSchema
import graphql.schema.FieldCoordinates.coordinates as makeFieldCoordinates

internal class NadelRenameTransform : NadelTransform<State> {
    data class State(
        val instructions: Map<FieldCoordinates, NadelRenameFieldInstruction>,
        val objectTypesWithoutRename: List<String>,
        val aliasHelper: AliasHelper,
        val field: NormalizedField,
        val service: Service,
    )

    override suspend fun isApplicable(
        executionContext: NadelExecutionContext,
        overallSchema: GraphQLSchema,
        executionBlueprint: NadelExecutionBlueprint,
        services: Map<String, Service>,
        service: Service,
        field: NormalizedField,
    ): State? {
        val renameInstructions = executionBlueprint.fieldInstructions
            .getInstructionsOfTypeForField<NadelRenameFieldInstruction>(field)
        if (renameInstructions.isEmpty()) {
            return null
        }

        val objectsWithoutRename = field.objectTypeNames.filterNot {
            makeFieldCoordinates(it, field.name) in renameInstructions
        }

        return State(
            renameInstructions,
            objectsWithoutRename,
            AliasHelper("my_uuid"),
            field,
            service,
        )
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
            newField = if (state.objectTypesWithoutRename.isNotEmpty()) {
                field.toBuilder()
                    .clearObjectTypesNames()
                    .objectTypeNames(state.objectTypesWithoutRename)
                    .build()
            } else {
                null
            },
            artificialFields = makeRenamedFields(state, transformer, executionPlan) + makeTypeNameField(state),
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
        return NadelTransformUtil.makeTypeNameField(
            aliasHelper = state.aliasHelper,
            objectTypeNames = state.instructions.keys.map { it.typeName },
        )
    }

    private suspend fun makeRenamedFields(
        state: State,
        transformer: NadelQueryTransformer.Continuation,
        executionPlan: NadelExecutionPlan,
    ): List<NormalizedField> {
        return state.instructions.map { (coordinates, instruction) ->
            makeRenamedField(
                state,
                transformer,
                executionPlan,
                state.service,
                state.field,
                coordinates,
                rename = instruction,
            )
        }
    }

    private suspend fun makeRenamedField(
        state: State,
        transformer: NadelQueryTransformer.Continuation,
        executionPlan: NadelExecutionPlan,
        service: Service,
        field: NormalizedField,
        fieldCoordinates: FieldCoordinates,
        rename: NadelRenameFieldInstruction,
    ): NormalizedField {
        val underlyingTypeName = executionPlan.getUnderlyingTypeName(fieldCoordinates.typeName)
        val underlyingObjectType = service.underlyingSchema.getObjectType(underlyingTypeName)
            ?: error("No underlying object type")
        return state.aliasHelper.toArtificial(
            createField(
                schema = service.underlyingSchema,
                parentType = underlyingObjectType,
                queryPathToField = QueryPath(rename.underlyingName),
                fieldArguments = emptyMap(),
                fieldChildren = transformer.transform(field.children),
            )
        )
    }

    override suspend fun getResultInstructions(
        executionContext: NadelExecutionContext,
        overallSchema: GraphQLSchema,
        executionPlan: NadelExecutionPlan,
        service: Service,
        field: NormalizedField, // Overall field
        result: ServiceExecutionResult,
        state: State,
    ): List<NadelResultInstruction> {
        val parentNodes = JsonNodeExtractor.getNodesAt(
            result.data,
            field.queryPath.dropLast(1),
        )

        return parentNodes.mapNotNull instruction@{ parentNode ->
            val instruction = state.instructions.getInstructionForNode(
                executionPlan = executionPlan,
                aliasHelper = state.aliasHelper,
                parentNode = parentNode,
            ) ?: return@instruction null

            val queryPathForSourceField = QueryPath(state.aliasHelper.getResultKey(instruction.underlyingName))
            val sourceFieldNode = JsonNodeExtractor.getNodesAt(parentNode, queryPathForSourceField)
                .emptyOrSingle() ?: return@instruction null

            NadelResultInstruction.Copy(
                subjectPath = sourceFieldNode.resultPath,
                destinationPath = parentNode.resultPath + field.resultKey,
            )
        }
    }
}

