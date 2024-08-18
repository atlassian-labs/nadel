package graphql.nadel.engine.transform.result.json

import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.util.AnyList
import graphql.nadel.engine.util.AnyMap
import graphql.nadel.engine.util.JsonMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Generic interface to extract a [JsonNode] from the result for a given [NadelQueryPath].
 *
 * Use [NadelCachingJsonNodes] for the most part because that is faster.
 * It is the default implementation.
 */

interface JsonNodes {
    /**
     * Extracts the nodes at the given query selection path.
     */
    fun getNodesAt(queryPath: NadelQueryPath, flatten: Boolean = false): List<JsonNode>

    companion object {
        internal var nodesFactory: (JsonMap) -> JsonNodes = {
            NadelCachingJsonNodes(it)
        }

        operator fun invoke(data: JsonMap): JsonNodes {
            return nodesFactory(data)
        }
    }
}

/**
 * Utility class to extract data out of the given [data].
 */
class NadelCachingJsonNodes(
    private val data: JsonMap,
    private val prefix: List<String>? = null, // [defer]
) : JsonNodes {
    private val nodes = ConcurrentHashMap<NadelQueryPath, List<JsonNode>>()
    private var removedPrefix: Boolean = false

    override fun getNodesAt(queryPath: NadelQueryPath, flatten: Boolean): List<JsonNode> {
        val rootNode = JsonNode(data)
        return getNodesAt(rootNode, queryPath, flatten)
    }

    /**
     * Extracts the nodes at the given query selection path.
     */
    private fun getNodesAt(rootNode: JsonNode, queryPath: NadelQueryPath, flatten: Boolean = false): List<JsonNode> {

        // if incremental, remove prefix
        if (prefix != null && !removedPrefix) {
            if (queryPath.startsWith(prefix.toList())) {
                removedPrefix = true
                getNodesAt(queryPath.removePrefix(prefix), flatten)
            } else {
                emptyList()
            }
        }
        removedPrefix = false

        // todo: check if this is valid
        // if (prefix != null && queryPath.startsWith(prefix.toList())) {
        //         getNodesAt(queryPath.removePrefix(prefix), flatten)
        // }

        var queue = listOf(rootNode)

        // todo work backwards here instead of forwards
        for (index in queryPath.segments.indices) {
            val subPath = NadelQueryPath(
                queryPath.segments.subList(0, index + 1) // +1 as endIndex is exclusive
            )
            val hasMore = index < queryPath.segments.lastIndex
            val pathSegment = queryPath.segments[index]

            queue = if (hasMore || flatten) {
                // nodes map populated here
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

    private fun getNodes(node: JsonNode, segment: String, flattenLists: Boolean): Sequence<JsonNode> {
        return when (node.value) {
            is AnyMap -> getNodes(node.value, segment, flattenLists)
            null -> emptySequence()
            else -> throw IllegalNodeTypeException(node)
        }
    }

    private fun getNodes(
        map: AnyMap,
        segment: String,
        flattenLists: Boolean,
    ): Sequence<JsonNode> {


        if (prefix != null && prefix.last() == segment){
                return sequenceOf(
                    JsonNode(map),
                )
        }
        val value = map[segment]

        // We flatten lists as these nodes contribute to the BFS queue
        if (value is AnyList && flattenLists) {
            return getFlatNodes(value)
        }


        return sequenceOf(
            JsonNode(value = value),
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
    private fun getFlatNodes(
        values: AnyList,
    ): Sequence<JsonNode> {
        return values
            .asSequence()
            .flatMap { value ->
                when (value) {
                    is AnyList -> getFlatNodes(value)
                    else -> sequenceOf(JsonNode(value = value))
                }
            }
    }
}
