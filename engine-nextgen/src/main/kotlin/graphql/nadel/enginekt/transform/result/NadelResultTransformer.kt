package graphql.nadel.enginekt.transform.result

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.result.NadelResultTransformer.DataMutation
import graphql.nadel.enginekt.transform.result.json.AnyJsonNodePathSegment
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodeExtractor
import graphql.nadel.enginekt.transform.result.json.JsonNodePath
import graphql.nadel.enginekt.transform.result.json.JsonNodePathSegment
import graphql.nadel.enginekt.util.AnyList
import graphql.nadel.enginekt.util.AnyMap
import graphql.nadel.enginekt.util.AnyMutableList
import graphql.nadel.enginekt.util.AnyMutableMap
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.enginekt.util.MutableJsonMap
import graphql.nadel.enginekt.util.queryPath
import graphql.normalized.ExecutableNormalizedField
import kotlin.reflect.KClass

internal class NadelResultTransformer(private val executionBlueprint: NadelOverallExecutionBlueprint) {
    suspend fun transform(
        executionContext: NadelExecutionContext,
        executionPlan: NadelExecutionPlan,
        artificialFields: List<ExecutableNormalizedField>,
        overallToUnderlyingFields: Map<ExecutableNormalizedField, List<ExecutableNormalizedField>>,
        service: Service,
        result: ServiceExecutionResult,
    ): ServiceExecutionResult {
        val instructions = executionPlan.transformationSteps.flatMap { (field, steps) ->
            steps.flatMap step@{ step ->
                // This can be null if we did not end up sending the field e.g. for hydration
                val underlyingFields = overallToUnderlyingFields[field]
                    ?.takeIf(List<ExecutableNormalizedField>::isNotEmpty)
                    ?: return@step emptyList()

                step.transform.getResultInstructions(
                    executionContext,
                    executionBlueprint,
                    service,
                    field,
                    underlyingFields.first().parent,
                    result,
                    step.state,
                )
            }
        } + getRemoveArtificialFieldInstructions(result, artificialFields)

        mutate(result, instructions)

        return result
    }

    private fun mutate(result: ServiceExecutionResult, instructions: List<NadelResultInstruction>) {
        // For now we don't have any instructions to modify anything other than data, so return early
        if (result.data == null) {
            return
        }

        val mutations = instructions.map { transformation ->
            when (transformation) {
                is NadelResultInstruction.Set -> prepareSet(result.data, transformation)
                is NadelResultInstruction.Remove -> prepareRemove(result.data, transformation)
                is NadelResultInstruction.Copy -> prepareCopy(result.data, transformation)
                is NadelResultInstruction.AddError -> prepareAddError(result.errors, transformation)
            }
        }

        val transformContext = TransformContext()
        mutations.forEach {
            it.run(context = transformContext)
        }

        // TODO: investigate whether this code is actually needed. So far no tests fail after disabling it…
        // transformContext.emptyPaths.forEach(result.data::cleanup)
    }

    private fun prepareSet(
        data: JsonMap,
        instruction: NadelResultInstruction.Set,
    ): DataMutation {
        val parentPath = instruction.subjectPath.dropLast(1)
        val parentMap = data.getJsonMapAt(parentPath) ?: return DataMutation {}
        val subjectKey = instruction.subjectKey

        return DataMutation { context ->
            modifyMap(context, parentPath, parentMap) {
                it[subjectKey] = instruction.newValue
            }
        }
    }

    private fun prepareRemove(
        data: JsonMap,
        instruction: NadelResultInstruction.Remove,
    ): DataMutation {
        val parentPath = instruction.subjectPath.dropLast(1)
        val parentMap = data.getJsonMapAt(parentPath) ?: return DataMutation {}
        val subjectKey = instruction.subjectKey

        val dataToDelete = parentMap[subjectKey]

        return DataMutation { context ->
            // Ensure that the node is still the same before removing it, ensures we don't remove the wrong thing
            if (parentMap[subjectKey] === dataToDelete) {
                modifyMap(context, parentPath, parentMap) {
                    it.remove(subjectKey)
                }
            }
        }
    }

    private fun prepareCopy(
        data: JsonMap,
        instruction: NadelResultInstruction.Copy,
    ): DataMutation {
        val parentMap = data.getParentOf(instruction.subjectPath) ?: return DataMutation {}
        val dataToCopy = parentMap[instruction.subjectKey]

        val destParentPath = instruction.destinationPath.dropLast(1)
        val destParentMap = data.getJsonMapAt(destParentPath) ?: return DataMutation {}
        val destKey = instruction.destinationKey

        return DataMutation { context ->
            modifyMap(context, destParentPath, destParentMap) {
                it[destKey] = dataToCopy
            }
        }
    }

    private fun prepareAddError(
        errors: List<JsonMap>,
        instruction: NadelResultInstruction.AddError,
    ): DataMutation {
        val newError = instruction.error.toSpecification()

        return DataMutation {
            val mutableErrors = errors.asMutable()
            mutableErrors.add(newError)
        }
    }

    private inline fun modifyMap(
        context: TransformContext,
        mapPath: JsonNodePath,
        map: JsonMap,
        modification: (MutableJsonMap) -> Unit,
    ) {
        val wasEmpty = map.isEmpty()
        modification(map.asMutable())

        if (map.isEmpty()) {
            if (!wasEmpty) {
                context.emptyPaths.add(mapPath)
            }
        } else if (wasEmpty) {
            context.emptyPaths.remove(mapPath)
        }
    }

    private fun interface DataMutation {
        fun run(context: TransformContext)
    }

    private fun getRemoveArtificialFieldInstructions(
        result: ServiceExecutionResult,
        artificialFields: List<ExecutableNormalizedField>,
    ): List<NadelResultInstruction> {
        return artificialFields.flatMap { field ->
            JsonNodeExtractor.getNodesAt(
                data = result.data,
                queryPath = field.queryPath,
            ).map { jsonNode ->
                NadelResultInstruction.Remove(
                    subjectPath = jsonNode.resultPath,
                )
            }
        }
    }

    private data class TransformContext(
        val emptyPaths: HashSet<JsonNodePath> = hashSetOf(),
    )
}

internal fun <K, V> Map<K, V>.asMutable(): MutableMap<K, V> {
    return this as? MutableMap<K, V> ?: throw NotMutableError()
}

private fun <T> List<T>.asMutable(): MutableList<T> {
    return this as? MutableList<T> ?: throw NotMutableError()
}

/**
 * This gets the object right before the last segment of the [node], aka the parent.
 */
private fun JsonMap.getParentOf(node: JsonNodePath): JsonMap? {
    val pathToParent = node.dropLast(1)
    return getJsonMapAt(pathToParent)
}

private fun JsonMap.getJsonMapAt(path: JsonNodePath): JsonMap? {
    @Suppress("UNCHECKED_CAST")
    return getAt(path).value as JsonMap?
}

private fun JsonMap.getAt(path: JsonNodePath): JsonNode {
    return path.segments.fold(JsonNode(JsonNodePath.root, this), JsonNode::get)
}

/**
 * This cleans up a the GraphQL response when a field is removed.
 *
 * If we remove a field from the tree and it is has no siblings, then we need to remove
 * the parent field too, and so on.
 */
private fun JsonMap.cleanup(path: JsonNodePath) {
    // This gets the node at every path segment up until the node specified by the path
    val items = path.segments.runningFold(JsonNode(JsonNodePath.root, this), JsonNode::get)

    // We work backwards here and see if a child has been deleted from the Map
    // If the map is empty, we want to remove it from the
    var childKeyToDelete: AnyJsonNodePathSegment? = null
    for (item in items.asReversed()) {
        when (val value = item.value) {
            is AnyMutableMap -> {
                when (childKeyToDelete) {
                    null -> {
                    }
                    is JsonNodePathSegment.String -> {
                        value.remove(key = childKeyToDelete.value)
                    }
                    else -> throw UnsupportedOperationException("Unsupported key to delete from Map")
                }
                if (value.isEmpty()) {
                    childKeyToDelete = item.resultPath.segments.lastOrNull()
                } else {
                    break
                }
            }
            is AnyMutableList -> {
                when (childKeyToDelete) {
                    null -> {
                    }
                    is JsonNodePathSegment.Int -> break
                    else -> throw UnsupportedOperationException("Unsupported key to delete from List")
                }
            }
            is AnyMap, is AnyList -> {
                throw NotMutableError()
            }
            null -> {
            }
            else -> break
        }
    }
}

private fun JsonNode.get(segment: AnyJsonNodePathSegment): JsonNode {
    return getNextNode(this, segment)
}

private fun getNextNode(current: JsonNode, segment: AnyJsonNodePathSegment): JsonNode {
    return when (segment) {
        is JsonNodePathSegment.String -> {
            val path = current.resultPath + segment.value
            when (val value = current.value) {
                is AnyMap? -> JsonNode(
                    resultPath = path,
                    value = value?.get(segment.value),
                )
                else -> throw UnexpectedDataType(
                    path,
                    expected = Map::class,
                    actual = value?.javaClass,
                )
            }
        }
        is JsonNodePathSegment.Int -> {
            val path = current.resultPath + segment.value
            when (val value = current.value) {
                is AnyList? -> JsonNode(
                    resultPath = path,
                    value = value?.get(segment.value),
                )
                else -> throw UnexpectedDataType(
                    path,
                    expected = List::class,
                    actual = value?.javaClass,
                )
            }
        }
    }
}

private class NotMutableError : RuntimeException("Data was required to be mutable but was not")

private class UnexpectedDataType private constructor(message: String) : RuntimeException(message) {
    companion object {
        operator fun invoke(path: JsonNodePath, expected: KClass<*>, actual: Class<*>?): UnexpectedDataType {
            val pathStr = path.segments.joinToString("/") {
                it.value.toString()
            }
            val expectedStr = expected.qualifiedName
            val actualStr = actual?.name ?: "null"
            val message = "Expected data at '$pathStr' to be of type '$expectedStr' but got '$actualStr'"
            return UnexpectedDataType(message)
        }
    }
}
