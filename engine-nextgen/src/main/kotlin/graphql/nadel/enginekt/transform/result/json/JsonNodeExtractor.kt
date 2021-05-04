package graphql.nadel.enginekt.transform.result.json

import graphql.nadel.enginekt.util.AnyList
import graphql.nadel.enginekt.util.AnyMap
import graphql.nadel.enginekt.util.JsonMap

/**
 * Use the [getNodesAt] function to extract get the nodes at the given query selection path.
 */
object JsonNodeExtractor {
    fun getNodesAt(data: JsonMap, queryResultKeyPath: List<String>, flatten: Boolean = false): List<JsonNode> {
        val rootNode = JsonNode(JsonNodePath.root, data)
        return getNodesAt(rootNode, queryResultKeyPath, flatten)
    }

    fun getNodesAt(rootNode: JsonNode, queryResultKeyPath: List<String>, flatten: Boolean = false): List<JsonNode> {
        // This is a breadth-first search
        return queryResultKeyPath.foldIndexed(listOf(rootNode)) { index, queue, pathSegment ->
            val atEnd = index == queryResultKeyPath.lastIndex
            // For all the nodes, get the next node according to the segment value
            // We use flatMap as one node may be a list with more than one node to explore
            queue.flatMap { node ->
                // At the end when we see lists we do NOT want to flatten them for BFS queue
                getNodes(node, pathSegment, flattenLists = !atEnd || flatten)
            }
        }
    }

    private fun getNodes(node: JsonNode, segment: String, flattenLists: Boolean): List<JsonNode> {
        return when (node.value) {
            is AnyMap -> getNodes(node.path, node.value, segment, flattenLists)
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
                null -> emptyList()
                else -> listOf(JsonNode(newPath, value))
            }
        }
    }
}

private class IllegalNodeTypeException private constructor(message: String) : RuntimeException(message) {
    companion object {
        operator fun invoke(node: JsonNode): IllegalNodeTypeException {
            val nodeType = node.value?.javaClass?.name ?: "null"
            val pathString = node.path.segments.joinToString("/") {
                it.value.toString()
            }

            return IllegalNodeTypeException("Unknown node type '$nodeType' at '$pathString' was not a map")
        }
    }
}
