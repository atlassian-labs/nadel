package graphql.nadel.engine.transform

import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.nadel.engine.blueprint.NadelRenameFieldInstruction
import graphql.nadel.engine.transform.NadelRenameTransform.TransformFieldContext
import graphql.nadel.engine.transform.NadelRenameTransform.TransformOperationContext
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

internal class NadelRenameTransform : NadelTransform<TransformOperationContext, TransformFieldContext> {
    internal data class TransformOperationContext(
        override val parentContext: NadelOperationExecutionContext,
    ) : NadelTransformOperationContext()

    internal data class TransformFieldContext(
        override val parentContext: TransformOperationContext,
        override val overallField: ExecutableNormalizedField,
        val instructionsByObjectTypeNames: Map<String, NadelRenameFieldInstruction>,
        val objectTypesWithoutRename: Set<String>,
        val aliasHelper: NadelAliasHelper,
    ) : NadelTransformFieldContext<TransformOperationContext>()

    override suspend fun getTransformOperationContext(
        operationExecutionContext: NadelOperationExecutionContext,
    ): TransformOperationContext {
        return TransformOperationContext(operationExecutionContext)
    }

    override suspend fun getTransformFieldContext(
        transformContext: TransformOperationContext,
        overallField: ExecutableNormalizedField,
    ): TransformFieldContext? {
        val renameInstructions = transformContext.executionBlueprint
            .getTypeNameToInstructionMap<NadelRenameFieldInstruction>(overallField)
        if (renameInstructions.isEmpty()) {
            return null
        }

        val objectsWithoutRename = overallField.objectTypeNames
            .asSequence()
            .filterNot { it in renameInstructions }
            .toHashSet()

        return TransformFieldContext(
            transformContext,
            overallField,
            renameInstructions,
            objectsWithoutRename,
            NadelAliasHelper.forField(tag = "rename", overallField),
        )
    }

    override suspend fun transformField(
        transformContext: TransformFieldContext,
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
    ): NadelTransformFieldResult {
        return NadelTransformFieldResult(
            newField = if (transformContext.objectTypesWithoutRename.isNotEmpty()) {
                field.toBuilder()
                    .clearObjectTypesNames()
                    .objectTypeNames(field.objectTypeNames.filter { it in transformContext.objectTypesWithoutRename })
                    .build()
            } else {
                null
            },
            artificialFields = makeRenamedFields(transformContext, transformer, field).let {
                when (val typeNameField = makeTypeNameField(transformContext, field)) {
                    null -> it
                    else -> it + typeNameField
                }
            },
        )
    }

    /**
     * Read [TransformFieldContext.instructionsByObjectTypeNames]
     *
     * In the case that there are multiple [FieldCoordinates] for a single [ExecutableNormalizedField]
     * we need to know which type we are dealing with, so we use this to add a `__typename`
     * selection to determine the behavior on [transformResult].
     *
     * This detail is omitted from most examples in this file for simplicity.
     */
    private fun makeTypeNameField(
        transformContext: TransformFieldContext,
        field: ExecutableNormalizedField,
    ): ExecutableNormalizedField? {
        // No need for typename on top level field
        if (transformContext.overallField.queryPath.size == 1) {
            return null
        }

        val typeNamesWithInstructions = transformContext.instructionsByObjectTypeNames.keys
        val objectTypeNames = field.objectTypeNames
            .filter { it in typeNamesWithInstructions }
            .takeIf { it.isNotEmpty() }
            ?: return null

        return makeTypeNameField(
            aliasHelper = transformContext.aliasHelper,
            objectTypeNames = objectTypeNames,
            deferredExecutions = field.deferredExecutions,
        )
    }

    private suspend fun makeRenamedFields(
        transformContext: TransformFieldContext,
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
    ): List<ExecutableNormalizedField> {
        val setOfFieldObjectTypeNames = field.objectTypeNames.toSet()
        return transformContext.instructionsByObjectTypeNames
            .asSequence()
            .asFlow() // For coroutines
            .filter { (typeName) ->
                // Don't insert type renames for fields that were never asked for
                typeName in setOfFieldObjectTypeNames
            }
            .map { (typeName, instruction) ->
                makeRenamedField(
                    transformContext,
                    transformer,
                    field,
                    typeName,
                    rename = instruction,
                )
            }
            .toList()
    }

    private suspend fun makeRenamedField(
        transformContext: TransformFieldContext,
        transformer: NadelQueryTransformer,
        field: ExecutableNormalizedField,
        overallTypeName: String,
        rename: NadelRenameFieldInstruction,
    ): ExecutableNormalizedField {
        val service = transformContext.service
        val underlyingTypeName = transformContext.executionBlueprint.getUnderlyingTypeName(service, overallTypeName)
        val underlyingObjectType = service.underlyingSchema.getObjectType(underlyingTypeName)
            ?: error("No underlying object type")
        return transformContext.aliasHelper.toArtificial(
            createField(
                schema = service.underlyingSchema,
                parentType = underlyingObjectType,
                queryPathToField = NadelQueryPath(listOf(rename.underlyingName)),
                fieldArguments = field.normalizedArguments,
                fieldChildren = transformer.transform(field.children),
                deferredExecutions = field.deferredExecutions,
            )
        )
    }

    override suspend fun transformResult(
        transformContext: TransformFieldContext,
        underlyingParentField: ExecutableNormalizedField?,
        resultNodes: JsonNodes,
    ): List<NadelResultInstruction> {
        val overallField = transformContext.overallField

        val parentNodes = resultNodes.getNodesAt(
            queryPath = underlyingParentField?.queryPath ?: NadelQueryPath.root,
            flatten = true,
        )

        return parentNodes.mapNotNull instruction@{ parentNode ->
            val instruction = getInstructionForNode(
                transformContext = transformContext,
                parentNode = parentNode,
            ) ?: return@instruction null

            val queryPathForSourceField = NadelQueryPath(
                segments = listOf(
                    transformContext.aliasHelper.getResultKey(instruction.underlyingName),
                ),
            )
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
        transformContext: TransformFieldContext,
        parentNode: JsonNode,
    ): NadelRenameFieldInstruction? {
        // There can't be multiple instructions for a top level field
        if (transformContext.overallField.queryPath.size == 1) {
            return transformContext.instructionsByObjectTypeNames.values.single()
        }

        return transformContext.instructionsByObjectTypeNames.getInstructionForNode(
            executionBlueprint = transformContext.executionBlueprint,
            service = transformContext.service,
            aliasHelper = transformContext.aliasHelper,
            parentNode = parentNode,
        )
    }
}

