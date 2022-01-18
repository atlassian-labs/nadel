package graphql.nadel.enginekt.transform.hydration

import graphql.nadel.Service
import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelGenericHydrationInstruction
import graphql.nadel.enginekt.blueprint.NadelHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationActorInputDef
import graphql.nadel.enginekt.transform.GraphQLObjectTypeName
import graphql.nadel.enginekt.transform.artificial.NadelAliasHelper
import graphql.nadel.enginekt.transform.hydration.batch.NadelBatchHydrationInputBuilder
import graphql.nadel.enginekt.transform.hydration.batch.NadelBatchHydrationObjectIdFieldBuilder.makeObjectIdField
import graphql.nadel.enginekt.transform.query.NFUtil
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.util.CountedBox
import graphql.nadel.enginekt.util.deepClone
import graphql.nadel.hooks.ServiceExecutionHooks
import graphql.normalized.ExecutableNormalizedField
import graphql.normalized.NormalizedInputValue

internal object NadelHydrationFieldsBuilder {
    fun makeActorQueries(
        instruction: NadelHydrationFieldInstruction,
        aliasHelper: NadelAliasHelper,
        fieldToHydrate: ExecutableNormalizedField,
        parentNode: JsonNode,
    ): List<CountedBox<ExecutableNormalizedField>> {
        return NadelHydrationInputBuilder.getInputValues(
            instruction = instruction,
            aliasHelper = aliasHelper,
            fieldToHydrate = fieldToHydrate,
            parentNode = parentNode,
        ).map { boxedArgs ->
            var counter = 0
            val args = boxedArgs.mapValues {
                counter += it.value.count
                it.value.boxedObject
            }
            val executableField = makeActorQueries(
                instruction = instruction,
                fieldArguments = args,
                fieldChildren = deepClone(fields = fieldToHydrate.children),
            )
            CountedBox(executableField,counter)
        }
    }

    fun makeBatchActorQueries(
        executionBlueprint: NadelOverallExecutionBlueprint,
        instruction: NadelBatchHydrationFieldInstruction,
        aliasHelper: NadelAliasHelper,
        hydratedField: ExecutableNormalizedField,
        parentNodes: List<JsonNode>,
        hooks: ServiceExecutionHooks,
    ): List<CountedBox<ExecutableNormalizedField>> {
        val boxedArgBatches = NadelBatchHydrationInputBuilder.getInputValueBatches(
            instruction = instruction,
            aliasHelper = aliasHelper,
            hydrationField = hydratedField,
            parentNodes = parentNodes,
        )

        val fieldChildren = deepClone(fields = hydratedField.children).let { children ->
            when (val objectIdField = makeObjectIdField(executionBlueprint, aliasHelper, instruction)) {
                null -> children
                else -> children + objectIdField
            }
        }

        return boxedArgBatches.map { boxedArg ->
            var counter = 0
            val argBatch = boxedArg.mapValues {
                counter += it.value.count
                it.value.boxedObject
            }

            val executableNormalizedField = makeActorQueries(
                instruction = instruction,
                fieldArguments = argBatch.mapKeys { (inputDef: NadelHydrationActorInputDef) -> inputDef.name },
                fieldChildren = fieldChildren,
            )
            CountedBox(executableNormalizedField,counter)
        }
    }

    fun makeFieldsUsedAsActorInputValues(
        service: Service,
        executionBlueprint: NadelOverallExecutionBlueprint,
        aliasHelper: NadelAliasHelper,
        objectTypeName: GraphQLObjectTypeName,
        instructions: List<NadelGenericHydrationInstruction>,
    ): List<ExecutableNormalizedField> {
        val underlyingTypeName = executionBlueprint.getUnderlyingTypeName(service, overallTypeName = objectTypeName)
        val underlyingObjectType = service.underlyingSchema.getObjectType(underlyingTypeName)
            ?: error("No underlying object type")

        return instructions.asSequence()
            .flatMap { it.actorInputValueDefs }
            .map { it.valueSource }
            .filterIsInstance<NadelHydrationActorInputDef.ValueSource.FieldResultValue>()
            .map { valueSource ->
                aliasHelper.toArtificial(
                    NFUtil.createField(
                        schema = service.underlyingSchema,
                        parentType = underlyingObjectType,
                        queryPathToField = valueSource.queryPathToField,
                        fieldArguments = emptyMap(),
                        fieldChildren = emptyList(), // This must be a leaf node
                    ),
                )
            }
            .toList()
    }

    private fun makeActorQueries(
        instruction: NadelGenericHydrationInstruction,
        fieldArguments: Map<String, NormalizedInputValue>,
        fieldChildren: List<ExecutableNormalizedField>,
    ): ExecutableNormalizedField {
        return NFUtil.createField(
            schema = instruction.actorService.underlyingSchema,
            parentType = instruction.actorService.underlyingSchema.queryType,
            queryPathToField = instruction.queryPathToActorField,
            fieldArguments = fieldArguments,
            fieldChildren = fieldChildren,
        )
    }
}
