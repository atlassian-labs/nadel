package graphql.nadel.engine.transform.hydration

import graphql.nadel.NadelEngineContext
import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.engine.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.engine.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.engine.blueprint.hydration.NadelHydrationArgumentDef
import graphql.nadel.engine.transform.GraphQLObjectTypeName
import graphql.nadel.engine.transform.hydration.NadelHydrationInputBuilder.Companion.getInputValues
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationInputBuilder
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationObjectIdFieldBuilder.makeObjectIdFields
import graphql.nadel.engine.transform.query.NFUtil
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.deepClone
import graphql.nadel.engine.util.resolveObjectTypes
import graphql.nadel.engine.util.toBuilder
import graphql.nadel.engine.util.unwrapAll
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.nadel.engine.transform.hydration.batch.NadelBatchHydrationTransform.NadelBatchHydrationContext as BatchTransformContext

internal object NadelHydrationFieldsBuilder {
    context(NadelEngineContext, NadelExecutionContext, NadelHydrationTransformContext)
    fun makeEffectQueries(
        instruction: NadelHydrationFieldInstruction,
        parentNode: JsonNode,
    ): List<ExecutableNormalizedField> {
        return getInputValues(
            instruction = instruction,
            parentNode = parentNode,
        ).map { args ->
            makeEffectQuery(
                instruction = instruction,
                fieldArguments = args,
                fieldChildren = deepClone(fields = hydrationCauseField.children),
            )
        }
    }

    context(NadelEngineContext, NadelExecutionContext, BatchTransformContext)
    fun makeBatchEffectQueries(
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
    ): List<ExecutableNormalizedField> {
        val argBatches = NadelBatchHydrationInputBuilder.getInputValueBatches(
            instruction = instruction,
            parentNodes = parentNodes,
        )

        val effectFieldOverallObjectTypeNames = getEffectFieldOverallObjectTypenames(instruction)
        val fieldChildren = deepClone(fields = hydrationCauseField.children)
            .mapNotNull { childField ->
                val objectTypesAreNotReturnedByEffectField =
                    effectFieldOverallObjectTypeNames.none { it in childField.objectTypeNames }

                if (objectTypesAreNotReturnedByEffectField) {
                    null
                } else {
                    childField.toBuilder()
                        .clearObjectTypesNames()
                        .objectTypeNames(childField.objectTypeNames.filter { it in effectFieldOverallObjectTypeNames })
                        .build()
                }
            }
            .let { children ->
                children + makeObjectIdFields(instruction)
            }

        return argBatches.map { argBatch ->
            makeEffectQuery(
                instruction = instruction,
                fieldArguments = argBatch.mapKeys { (inputDef: NadelHydrationArgumentDef) -> inputDef.name },
                fieldChildren = fieldChildren,
            )
        }
    }

    context(NadelEngineContext, NadelExecutionContext, NadelHydrationTransformContext)
    private fun getEffectFieldOverallObjectTypenames(
        instruction: NadelBatchHydrationFieldInstruction,
    ): Set<String> {
        val overallTypeName = instruction.effectFieldDef.type.unwrapAll().name

        val overallType = overallSchema.getType(overallTypeName)
            ?: error("Unable to find overall type $overallTypeName")

        val effectFieldOverallObjectTypes = resolveObjectTypes(overallSchema, overallType) { type ->
            error("Unable to resolve to object type: $type")
        }

        return effectFieldOverallObjectTypes
            .asSequence()
            .map { it.name }
            .toSet()
    }

    context(NadelEngineContext, NadelExecutionContext, NadelHydrationTransformContext)
    fun makeJoiningFields(
        objectTypeName: GraphQLObjectTypeName,
        instructions: List<NadelGenericHydrationInstruction>,
    ): List<ExecutableNormalizedField> {
        val underlyingTypeName = executionBlueprint.getUnderlyingTypeName(
            service = hydrationCauseService,
            overallTypeName = objectTypeName,
        )
        val underlyingObjectType = hydrationCauseService.underlyingSchema.getObjectType(underlyingTypeName)
            ?: error("No underlying object type")

        return instructions
            .asSequence()
            .flatMap { it.joiningFields }
            .map {
                aliasHelper.toArtificial(
                    NFUtil.createField(
                        schema = hydrationCauseService.underlyingSchema,
                        parentType = underlyingObjectType,
                        queryPathToField = it,
                        fieldArguments = emptyMap(),
                        fieldChildren = emptyList(), // This must be a leaf node
                    ),
                )
            }
            .toList()
    }

    context(NadelEngineContext, NadelExecutionContext, NadelHydrationTransformContext)
    private fun makeEffectQuery(
        instruction: NadelGenericHydrationInstruction,
        fieldArguments: Map<String, NormalizedInputValue>,
        fieldChildren: List<ExecutableNormalizedField>,
    ): ExecutableNormalizedField {
        return NFUtil.createField(
            schema = overallSchema,
            parentType = overallSchema.queryType,
            queryPathToField = instruction.queryPathToEffectField,
            fieldArguments = fieldArguments,
            fieldChildren = fieldChildren,
        )
    }
}
