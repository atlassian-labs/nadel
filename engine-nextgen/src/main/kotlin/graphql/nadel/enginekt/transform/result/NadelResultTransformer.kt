package graphql.nadel.enginekt.transform.result

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.NadelExecutionContext
import graphql.nadel.enginekt.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.result.NadelResultTransformer.DataMutation
import graphql.nadel.enginekt.transform.result.json.AnyJsonNodePathSegment
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodePath
import graphql.nadel.enginekt.transform.result.json.JsonNodePathSegment
import graphql.nadel.enginekt.transform.result.json.JsonNodes
import graphql.nadel.enginekt.util.AnyList
import graphql.nadel.enginekt.util.AnyMap
import graphql.nadel.enginekt.util.AnyMutableList
import graphql.nadel.enginekt.util.AnyMutableMap
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.enginekt.util.MutableJsonMap
import graphql.nadel.enginekt.util.asMutableJsonMap
import graphql.nadel.enginekt.util.queryPath
import graphql.normalized.ExecutableNormalizedField
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
        val nodes = JsonNodes(result.data, executionContext.hints)

        val deferredInstructions = ArrayList<Deferred<List<NadelResultInstruction>>>()

        coroutineScope {
            for ((field, steps) in executionPlan.transformationSteps) {
                // This can be null if we did not end up sending the field e.g. for hydration
                val underlyingFields = overallToUnderlyingFields[field]
                if (underlyingFields == null || underlyingFields.isEmpty()) {
                    continue
                }

                for (step in steps) {
                    deferredInstructions.add(
                        async {
                            step.transform.getResultInstructions(
                                executionContext,
                                executionBlueprint,
                                service,
                                field,
                                underlyingFields.first().parent,
                                result,
                                step.state,
                                nodes,
                            )
                        },
                    )
                }
            }
        }

        val instructions = deferredInstructions
            .awaitAll()
            .flatten() +
            getRemoveArtificialFieldInstructions(artificialFields, nodes)

        mutate(result, instructions)

        return result
    }

    private fun mutate(result: ServiceExecutionResult, instructions: List<NadelResultInstruction>) {
        // For now, we don't have any instructions to modify anything other than data, so return early
        if (result.data == null) {
            return
        }

        val mutations = instructions.mapNotNull { transformation ->
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
    ): DataMutation? {
        return prepareSet(
            data,
            destPath = instruction.subjectPath,
            newValue = instruction.newValue,
        )
    }

    private fun prepareRemove(
        data: JsonMap,
        instruction: NadelResultInstruction.Remove,
    ): DataMutation? {
        val parentPath = instruction.subjectPath.dropLast(1)
        val parentMap = data.getJsonMapAt(parentPath) ?: return null
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
    ): DataMutation? {
        val dataToCopy = data.getAt(instruction.subjectPath)
        return prepareSet(
            data,
            destPath = instruction.destinationPath,
            newValue = dataToCopy.value,
        )
    }

    private fun prepareSet(
        data: JsonMap,
        destPath: JsonNodePath,
        newValue: Any?,
    ): DataMutation? {
        val destParentPath = destPath.dropLast(1)
        val destParentValue = data.getAt(destParentPath).value
        val destKey = destPath.segments.last().value

        return when (destParentValue) {
            is AnyMap -> DataMutation {
                when (destKey) {
                    is String -> destParentValue.asMutableJsonMap()[destKey] = newValue
                    else -> error("Set instruction expected string key for JSON object")
                }
            }
            is AnyList -> DataMutation {
                when (destKey) {
                    is Int -> destParentValue.asMutable()[destKey] = newValue
                    else -> error("Set instruction expected integer key for JSON array")
                }
            }
            null -> null
            else -> error("Set instruction had leaf value as parent of destination path")
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
        artificialFields: List<ExecutableNormalizedField>,
        nodes: JsonNodes,
    ): List<NadelResultInstruction> {
        return artificialFields
            .asSequence()
            .flatMap { field ->
                nodes.getNodesAt(
                    queryPath = field.queryPath,
                ).map { jsonNode ->
                    NadelResultInstruction.Remove(
                        subjectPath = jsonNode.resultPath,
                    )
                }
            }
            .toList()
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
    var cursor: Any = this

    for (segment in path.segments) {
        cursor = when (segment) {
            is JsonNodePathSegment.Int -> {
                if (cursor is AnyList) {
                    cursor[segment.value]
                } else {
                    error("Requires List")
                }
            }
            is JsonNodePathSegment.String -> {
                if (cursor is AnyMap) {
                    cursor[segment.value]
                } else {
                    error("Requires List")
                }
            }
        } ?: return JsonNode(path, null)
    }

    return JsonNode(
        path,
        cursor,
    )
    // return path.segments.fold(JsonNode(JsonNodePath.root, this), JsonNode::get)
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
