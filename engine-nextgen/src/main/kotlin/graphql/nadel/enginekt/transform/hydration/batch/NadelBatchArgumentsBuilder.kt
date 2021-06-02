package graphql.nadel.enginekt.transform.hydration.batch

import graphql.nadel.enginekt.blueprint.NadelBatchHydrationFieldInstruction
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationArgument
import graphql.nadel.enginekt.blueprint.hydration.NadelHydrationArgumentValueSource
import graphql.nadel.enginekt.transform.hydration.NadelHydrationArgumentsBuilder.valueToAstValue
import graphql.nadel.enginekt.transform.hydration.NadelHydrationUtil
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.util.emptyOrSingle
import graphql.nadel.enginekt.util.flatten
import graphql.nadel.enginekt.util.mapFrom
import graphql.normalized.NormalizedField
import graphql.normalized.NormalizedInputValue
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLTypeUtil

object NadelBatchArgumentsBuilder {
    fun getFieldValues(
        instruction: NadelBatchHydrationFieldInstruction,
        hydrationField: NormalizedField,
        parentNodes: List<JsonNode>,
    ): List<Map<NadelHydrationArgument, NormalizedInputValue>> {
        val sourceFieldDefinition = NadelHydrationUtil.getSourceFieldDefinition(instruction)

        val nonBatchArgs = getNonBatchArgs(instruction, hydrationField)
        val batchArgs = getBatchArgs(sourceFieldDefinition, instruction, parentNodes)

        return batchArgs.map { nonBatchArgs + batchArgs }
    }

    private fun getNonBatchArgs(
        instruction: NadelBatchHydrationFieldInstruction,
        hydrationField: NormalizedField,
    ): Map<NadelHydrationArgument, NormalizedInputValue> {
        return mapFrom(
            instruction.sourceFieldArguments.mapNotNull { sourceFieldArg ->
                when (val valueSource = sourceFieldArg.valueSource) {
                    is NadelHydrationArgumentValueSource.ArgumentValue -> {
                        when (val argValue = hydrationField.normalizedArguments[valueSource.argumentName]) {
                            null -> null
                            else -> sourceFieldArg to argValue
                        }
                    }
                    is NadelHydrationArgumentValueSource.FieldValue -> null
                }
            },
        )
    }

    private fun getBatchArgs(
        sourceFieldDefinition: GraphQLFieldDefinition,
        instruction: NadelBatchHydrationFieldInstruction,
        parentNodes: List<JsonNode>,
    ): List<Pair<NadelHydrationArgument, NormalizedInputValue>> {
        val batchSize = instruction.batchSize

        val (batchArg, valueSource) = instruction.sourceFieldArguments
            .asSequence()
            .mapNotNull {
                when (val valueSource = it.valueSource) {
                    is NadelHydrationArgumentValueSource.FieldValue -> it to valueSource
                    else -> null
                }
            }
            .emptyOrSingle() ?: return emptyList()

        val batchArgDef = sourceFieldDefinition.getArgument(batchArg.name)

        return getFieldValues(valueSource, parentNodes)
            .chunked(size = batchSize)
            .map { chunk ->
                batchArg to NormalizedInputValue(
                    GraphQLTypeUtil.simplePrint(batchArgDef.type),
                    valueToAstValue(chunk),
                )
            }
    }

    private fun getFieldValues(
        valueSource: NadelHydrationArgumentValueSource.FieldValue,
        parentNodes: List<JsonNode>,
    ): List<Any?> {
        return parentNodes.flatMap { parentNode ->
            val nodes = JsonNodeExtractor.getNodesAt(
                rootNode = parentNode,
                queryResultKeyPath = valueSource.pathToField,
                flatten = true,
            )

            nodes.asSequence()
                .map { it.value }
                .flatten(recursively = true)
                .toList()
        }
    }
}
