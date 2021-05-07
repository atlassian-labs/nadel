package graphql.nadel.enginekt.transform.result

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionResult
import graphql.nadel.enginekt.blueprint.NadelExecutionBlueprint
import graphql.nadel.enginekt.plan.NadelExecutionPlan
import graphql.nadel.enginekt.transform.result.json.AnyJsonNodePathSegment
import graphql.nadel.enginekt.transform.result.json.JsonNode
import graphql.nadel.enginekt.transform.result.json.JsonNodePath
import graphql.nadel.enginekt.transform.result.json.JsonNodePathSegment
import graphql.nadel.enginekt.util.AnyList
import graphql.nadel.enginekt.util.AnyMap
import graphql.nadel.enginekt.util.AnyMutableList
import graphql.nadel.enginekt.util.AnyMutableMap
import graphql.nadel.enginekt.util.JsonMap
import graphql.schema.GraphQLSchema
import kotlin.reflect.KClass

internal class NadelResultTransformer(
    private val overallSchema: GraphQLSchema,
    private val executionBlueprint: NadelExecutionBlueprint,
) {
    fun transform(
        userContext: Any?,
        executionPlan: NadelExecutionPlan,
        service: Service,
        result: ServiceExecutionResult,
    ): ServiceExecutionResult {
        val instructions = executionPlan.resultTransformations.flatMap { (field, transforms) ->
            transforms.flatMap { transformation ->
                transformation.transform.getInstructions(
                    userContext,
                    overallSchema,
                    executionBlueprint,
                    service,
                    field,
                    result,
                )
            }
        }

        mutate(result, instructions)

        return result
    }

    private fun mutate(result: ServiceExecutionResult, transformations: List<NadelResultInstruction>) {
        val mutations = transformations.map { transformation ->
            when (transformation) {
                is NadelResultInstruction.Set -> prepareSet(result.data, transformation)
                is NadelResultInstruction.Remove -> prepareRemove(result.data, transformation)
                is NadelResultInstruction.Copy -> prepareCopy(result.data, transformation)
            }
        }

        mutations.forEach(DataMutation::run)

        // Clean up data at the end
        transformations
            .asSequence()
            .mapNotNull {
                when (it) {
                    is NadelResultInstruction.Remove -> it.subjectPath
                    else -> null
                }
            }
            .forEach(result.data::cleanup)
    }

    private fun prepareSet(data: JsonMap, instruction: NadelResultInstruction.Set): DataMutation {
        val parent = data.getParentOf(instruction.subjectPath) ?: return DataMutation {}
        val subjectKey = instruction.subjectKey

        return DataMutation {
            val mutableData = parent.asMutable()
            mutableData[subjectKey] = instruction.newValue
        }
    }

    private fun prepareRemove(data: JsonMap, instruction: NadelResultInstruction.Remove): DataMutation {
        val parent = data.getParentOf(instruction.subjectPath) ?: return DataMutation {}
        val subjectKey = instruction.subjectKey

        val dataToDelete = parent[subjectKey]

        return DataMutation {
            val mutableData = parent.asMutable()

            // Ensure that the node is still the same before removing it, ensures we don't remove the wrong thing
            if (mutableData[subjectKey] === dataToDelete) {
                mutableData.remove(subjectKey)
            }
        }
    }

    private fun prepareCopy(data: JsonMap, instruction: NadelResultInstruction.Copy): DataMutation {
        val parent = data.getParentOf(instruction.subjectPath) ?: return DataMutation {}
        val dataToCopy = parent[instruction.subjectKey]

        val destinationParent = data.getJsonMapAt(instruction.destinationPath.dropLast(1)) ?: return DataMutation {}
        val destinationKey = instruction.destinationKey

        return DataMutation {
            val mutableData = destinationParent.asMutable()
            mutableData[destinationKey] = dataToCopy
        }
    }

    private fun interface DataMutation {
        fun run()
    }
}

private fun <K, V> Map<K, V>.asMutable(): MutableMap<K, V> {
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
                    childKeyToDelete = item.path.segments.lastOrNull()
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
            val path = current.path + segment.value
            when (val value = current.value) {
                is AnyMap? -> JsonNode(
                    path = path,
                    value = value?.get(segment.value),
                )
                else -> throw UnexpectedDataType(
                    path,
                    expected = Map::class,
                    actual = value?.javaClass
                )
            }
        }
        is JsonNodePathSegment.Int -> {
            val path = current.path + segment.value
            when (val value = current.value) {
                is AnyList? -> JsonNode(
                    path = path,
                    value = value?.get(segment.value),
                )
                else -> throw UnexpectedDataType(
                    path,
                    expected = List::class,
                    actual = value?.javaClass
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
