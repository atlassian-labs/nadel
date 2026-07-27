package graphql.nadel.engine.transform.hydration

import graphql.nadel.NadelExecutionHints
import graphql.nadel.Service
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.blueprint.hydration.NadelHydrationArgument
import graphql.nadel.engine.transform.GraphQLObjectTypeName
import graphql.nadel.engine.transform.artificial.NadelAliasHelper
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationObjectIdFieldBuilder.makeObjectIdFields
import graphql.nadel.engine.transform.query.NFUtil
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.deepClone
import graphql.nadel.engine.util.toBuilder
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue

internal object NadelHydrationFieldsBuilder {
    fun makeBackingQueries(
        instruction: NadelHydrationFieldInstruction,
        aliasHelper: NadelAliasHelper,
        virtualField: ExecutableNormalizedField,
        parentNode: JsonNode,
        executionBlueprint: NadelOverallExecutionBlueprint,
    ): List<ExecutableNormalizedField> {
        return NadelHydrationInputBuilder
            .getInputValues(
                instruction = instruction,
                aliasHelper = aliasHelper,
                virtualField = virtualField,
                parentNode = parentNode,
            )
            .map { args ->
                makeBackingQueries(
                    instruction = instruction,
                    fieldArguments = args,
                    fieldChildren = deepClone(filterChildren(instruction, virtualField.children)),
                    executionBlueprint = executionBlueprint,
                )
            }
            // Fix types for virtual fields
            .onEach { field ->
                setBackingObjectTypeNames(instruction, field)
                field.traverseSubTree { child ->
                    setBackingObjectTypeNames(instruction, child)
                }
            }
    }

    fun makeBatchBackingQueries(
        executionBlueprint: NadelOverallExecutionBlueprint,
        instruction: NadelBatchHydrationFieldInstruction,
        aliasHelper: NadelAliasHelper,
        virtualField: ExecutableNormalizedField,
        argBatches: List<Map<NadelHydrationArgument, NormalizedInputValue>>,
    ): List<ExecutableNormalizedField> {
        val fieldChildren = deepClone(fields = filterChildren(instruction, virtualField.children)) +
            makeObjectIdFields(executionBlueprint, aliasHelper, instruction)

        return argBatches.map { argBatch ->
            makeBackingQueries(
                instruction = instruction,
                fieldArguments = argBatch.mapKeys { (argument) -> argument.name },
                fieldChildren = fieldChildren,
                executionBlueprint = executionBlueprint,
            )
        }
    }

    private fun filterChildren(
        instruction: NadelGenericHydrationInstruction,
        children: List<ExecutableNormalizedField>,
    ): List<ExecutableNormalizedField> {
        return children
            .mapNotNull { childField ->
                val legalObjectTypeNames =
                    childField.objectTypeNames.intersect(instruction.backingFieldReturnsObjectTypeNames)

                if (legalObjectTypeNames.isEmpty()) {
                    null
                } else if (legalObjectTypeNames == childField.objectTypeNames) {
                    childField
                } else {
                    childField.toBuilder()
                        .clearObjectTypesNames()
                        .objectTypeNames(legalObjectTypeNames.toList())
                        .build()
                }
            }
    }

    fun makeRequiredSourceFields(
        hints: NadelExecutionHints,
        service: Service,
        executionBlueprint: NadelOverallExecutionBlueprint,
        aliasHelper: NadelAliasHelper,
        objectTypeName: GraphQLObjectTypeName,
        instructions: List<NadelGenericHydrationInstruction>,
    ): List<ExecutableNormalizedField> {
        if (hints.hydrationExecutableSourceFields(service)) {
            return instructions
                .asSequence()
                .flatMap {
                    it.executableSourceFields
                }
                .map {
                    aliasHelper.toArtificial(it)
                }
                .toList()
        }

        val underlyingTypeName = executionBlueprint.getUnderlyingTypeName(service, overallTypeName = objectTypeName)
        val underlyingObjectType = service.underlyingSchema.getObjectType(underlyingTypeName)
            ?: error("No underlying object type")

        return instructions
            .asSequence()
            .flatMap { it.sourceFields }
            .map {
                aliasHelper.toArtificial(
                    NFUtil.createField(
                        schema = service.underlyingSchema,
                        parentType = underlyingObjectType,
                        queryPathToField = it,
                        fieldArguments = emptyMap(),
                        fieldChildren = emptyList(), // This must be a leaf node
                    ),
                )
            }
            .toList()
    }

    private fun makeBackingQueries(
        instruction: NadelGenericHydrationInstruction,
        fieldArguments: Map<String, NormalizedInputValue>,
        fieldChildren: List<ExecutableNormalizedField>,
        executionBlueprint: NadelOverallExecutionBlueprint,
    ): ExecutableNormalizedField {
        return NFUtil.createField(
            schema = executionBlueprint.engineSchema,
            parentType = executionBlueprint.engineSchema.queryType,
            queryPathToField = instruction.queryPathToBackingField,
            fieldArguments = fieldArguments,
            fieldChildren = fieldChildren,
        )
    }

    /**
     * This converts the [ExecutableNormalizedField.objectTypeNames] from the virtual types
     * to the backing types.
     */
    private fun setBackingObjectTypeNames(
        instruction: NadelHydrationFieldInstruction,
        field: ExecutableNormalizedField,
    ) {
        val virtualTypeToBackingType = instruction.virtualTypeContext?.virtualTypeToBackingType
            ?: return // Nothing to do

        field.objectTypeNames.forEach { virtualType ->
            val backingType = virtualTypeToBackingType[virtualType] ?: return@forEach
            field.objectTypeNames.remove(virtualType)
            field.objectTypeNames.add(backingType)
        }
    }
}
