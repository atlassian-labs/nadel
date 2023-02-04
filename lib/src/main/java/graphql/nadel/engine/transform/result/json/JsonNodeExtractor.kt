package graphql.nadel.engine.transform.result.json

import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.util.AnyList
import graphql.nadel.engine.util.AnyMap
import graphql.nadel.engine.util.JsonMap

@Deprecated("Start moving to JsonNodes for performance reasons")
object JsonNodeExtractor {
    /**
     * Extracts the nodes at the given query selection path.
     */
    fun getNodesAt(data: JsonMap, queryPath: NadelQueryPath, flatten: Boolean = false): List<JsonNode> {
        val rootNode = JsonNode(data)
        return getNodesAt(rootNode, queryPath, flatten)
    }

    /**
     * Extracts the nodes at the given query selection path.
     */
    fun getNodesAt(rootNode: JsonNode, queryPath: NadelQueryPath, flatten: Boolean = false): List<JsonNode> {
        if (queryPath.size == 1) {
            return getNodes(rootNode, queryPath.last(), flattenLists = flatten)
        }

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

    private fun getNodes(node: JsonNode, segment: String, flattenLists: Boolean): List<JsonNode> {
        return when (node.value) {
            is AnyMap -> getNodes(node.value, segment, flattenLists)
            null -> emptyList()
            else -> throw IllegalNodeTypeException(node)
        }
    }

    private fun getNodes(
        map: AnyMap,
        segment: String,
        flattenLists: Boolean,
    ): List<JsonNode> {
        val value = map[segment]

        // We flatten lists as these nodes contribute to the BFS queue
        if (value is AnyList && flattenLists) {
            return getFlatNodes(value)
        }

        return listOf(JsonNode(value))
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
    private fun getFlatNodes(values: AnyList): List<JsonNode> {
        return values.flatMap { value ->
            when (value) {
                is AnyList -> getFlatNodes(value)
                else -> listOf(JsonNode(value))
            }
        }
    }
}

class IllegalNodeTypeException private constructor(message: String) : RuntimeException(message) {
    companion object {
        operator fun invoke(node: JsonNode): IllegalNodeTypeException {
            val nodeType = node.value?.javaClass?.name ?: "null"
            // val pathString = node.resultPath.segments.joinToString("/") {
            //     it.value.toString()
            // }

            return IllegalNodeTypeException("Unknown node type '$nodeType' was not a map")
        }
    }
}
