package graphql.nadel.engine.transform.result.json

import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.util.AnyList
import graphql.nadel.engine.util.AnyMap
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.foldWhileNotNull
import java.util.Collections

/**
 * Utility class to extract data out of the given
 */
class JsonNodes(
    private val data: JsonMap,
    private val executionFlags: NadelExecutionHints,
) {
    private val nodes = Collections.synchronizedMap(
        mutableMapOf<NadelQueryPath, List<JsonNode>>(),
    )

    /**
     * Extracts the nodes at the given query selection path.
     */
    fun getNodesAt(queryPath: NadelQueryPath, flatten: Boolean = false): List<JsonNode> {
        val rootNode = JsonNode(JsonNodePath.root, data)
        return getNodesAt(rootNode, queryPath, flatten)
    }

    /**
     * Extract the node at the given json node path.
     */
    fun getNodeAt(path: JsonNodePath): JsonNode? {
        val rootNode = JsonNode(JsonNodePath.root, data)
        return getNodeAt(rootNode, path)
    }

    /**
     * Extracts the nodes at the given query selection path.
     */
    private fun getNodesAt(rootNode: JsonNode, queryPath: NadelQueryPath, flatten: Boolean = false): List<JsonNode> {
        var queue = listOf(rootNode)

        // todo work backwards here instead of forwards
        for (index in queryPath.segments.indices) {
            val subPath = NadelQueryPath(
                queryPath.segments.subList(0, index + 1) // +1 as endIndex is exclusive
            )
            val hasMore = index < queryPath.segments.lastIndex
            val pathSegment = queryPath.segments[index]

            queue = if (hasMore || flatten) {
                nodes.computeIfAbsent(subPath) {
                    queue.flatMap { node ->
                        getNodes(node, pathSegment, flattenLists = true)
                    }
                }
            } else {
                queue.flatMap { node ->
                    getNodes(node, pathSegment, flattenLists = flatten)
                }
            }
        }

        return queue
    }

    /**
     * Extract the node at the given json node path.
     */
    private fun getNodeAt(rootNode: JsonNode, path: JsonNodePath): JsonNode? {
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

    private fun getNodes(node: JsonNode, segment: String, flattenLists: Boolean): Sequence<JsonNode> {
        return when (node.value) {
            is AnyMap -> getNodes(node.resultPath, node.value, segment, flattenLists)
            null -> emptySequence()
            else -> throw IllegalNodeTypeException(node)
        }
    }

    private fun getNodes(
        parentPath: JsonNodePath,
        map: AnyMap,
        segment: String,
        flattenLists: Boolean,
    ): Sequence<JsonNode> {
        val newPath = parentPath + segment
        val value = map[segment]

        // We flatten lists as these nodes contribute to the BFS queue
        if (value is AnyList && flattenLists) {
            return getFlatNodes(parentPath = newPath, value)
        }

        return sequenceOf(
            JsonNode(newPath, value),
        )
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
    private fun getFlatNodes(parentPath: JsonNodePath, values: AnyList): Sequence<JsonNode> {
        return values
            .asSequence()
            .flatMapIndexed { index, value ->
                val newPath = parentPath + index
                when (value) {
                    is AnyList -> getFlatNodes(newPath, value)
                    else -> sequenceOf(
                        JsonNode(newPath, value),
                    )
                }
            }
    }
}
