package graphql.nadel.engine.transform.result.json

import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.util.AnyList
import graphql.nadel.engine.util.AnyMap
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.foldWhileNotNull

@Deprecated("Start moving to JsonNodes for performance reasons")
object JsonNodeExtractor {
    /**
     * Extracts the nodes at the given query selection path.
     */
    fun getNodesAt(data: JsonMap, queryPath: NadelQueryPath, flatten: Boolean = false): List<JsonNode> {
        val rootNode = JsonNode(JsonNodePath.root, data)
        return getNodesAt(rootNode, queryPath, flatten)
    }

    /**
     * Extracts the nodes at the given query selection path.
     */
    fun getNodesAt(rootNode: JsonNode, queryPath: NadelQueryPath, flatten: Boolean = false): List<JsonNode> {
        // This is a breadth-first search
        return queryPath.segments.foldIndexed(listOf(rootNode)) { index, queue, pathSegment ->
            val atEnd = index == queryPath.segments.lastIndex
            // For all the nodes, get the next node according to the segment value
            // We use flatMap as one node may be a list with more than one node to explore
            queue.flatMap { node ->
                // At the end when we see lists we do NOT want to flatten them for BFS queue
                getNodes(node, pathSegment, flattenLists = !atEnd || flatten)
            }
        }
    }

    /**
     * Extract the node at the given json node path.
     */
    fun getNodeAt(data: JsonMap, path: JsonNodePath): JsonNode? {
        val rootNode = JsonNode(JsonNodePath.root, data)
        return getNodeAt(rootNode, path)
    }

    /**
     * Extract the node at the given json node path.
     */
    fun getNodeAt(rootNode: JsonNode, path: JsonNodePath): JsonNode? {
        return path.segments.foldWhileNotNull(rootNode as JsonNode?) { currentNode, segment ->
            when (currentNode?.value) {
                is AnyMap -> currentNode.value[segment.value]?.let {
                    JsonNode(currentNode.resultPath + segment, it)
                }
                is AnyList -> when (segment) {
                    is JsonNodePathSegment.Int -> currentNode.value.getOrNull(segment.value)?.let {
                        JsonNode(currentNode.resultPath + segment, it)
                    }
                    else -> null
                }
                else -> null
            }
        }
    }

    private fun getNodes(node: JsonNode, segment: String, flattenLists: Boolean): List<JsonNode> {
        return when (node.value) {
            is AnyMap -> getNodes(node.resultPath, node.value, segment, flattenLists)
            null -> emptyList()
            else -> throw IllegalNodeTypeException(node)
        }
    }

    private fun getNodes(
        parentPath: JsonNodePath,
        map: AnyMap,
        segment: String,
        flattenLists: Boolean,
    ): List<JsonNode> {
        val newPath = parentPath + segment
        val value = map[segment]

        // We flatten lists as these nodes contribute to the BFS queue
        if (value is AnyList && flattenLists) {
            return getFlatNodes(parentPath = newPath, value)
        }

        return listOf(JsonNode(newPath, value))
    }

    /**
     * Collects [JsonMap] nodes inside a [List]. Effectively we call this function to remove
     * traces of [List]s.
     *
     * For example for the path /users we return the following nodes:
     *
     * `/users/[0]`
     *
     * `/users/[1]`
     *
     * etc.
     */
    private fun getFlatNodes(parentPath: JsonNodePath, values: AnyList): List<JsonNode> {
        return values.flatMapIndexed { index, value ->
            val newPath = parentPath + index
            when (value) {
                is AnyList -> getFlatNodes(newPath, value)
                else -> listOf(JsonNode(newPath, value))
            }
        }
    }
}

class IllegalNodeTypeException private constructor(message: String) : RuntimeException(message) {
    companion object {
        operator fun invoke(node: JsonNode): IllegalNodeTypeException {
            val nodeType = node.value?.javaClass?.name ?: "null"
            val pathString = node.resultPath.segments.joinToString("/") {
                it.value.toString()
            }

            return IllegalNodeTypeException("Unknown node type '$nodeType' at '$pathString' was not a map")
        }
    }
}
