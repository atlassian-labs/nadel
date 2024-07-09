package graphql.nadel.engine.transform.result.json

import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.result.json.NadelEphemeralJsonNode.Companion.component1
import graphql.nadel.engine.transform.result.json.NadelEphemeralJsonNode.Companion.component2
import graphql.nadel.engine.transform.result.json.NadelEphemeralJsonNode.Companion.component3
import graphql.nadel.engine.util.AnyList
import graphql.nadel.engine.util.AnyMap
import graphql.nadel.result.NadelResultPath
import graphql.nadel.result.NadelResultPathSegment

/**
 * In theory what should be the [JsonNodeExtractor] replacement.
 *
 * Though, replacement is a tall order they have different iteration patterns.
 *
 * Because of that, things like GraphQL queries to underlying services are different
 * and the tests need to be updated.
 *
 * So for now, I'll leave this in here in case a new feature uses it in the future.
 */
internal class NadelIteratingJsonNodes(
    private val data: Any?,
) : JsonNodes {
    override fun getNodesAt(queryPath: NadelQueryPath, flatten: Boolean): List<JsonNode> {
        val iterator = NadelJsonNodeIterator(
            root = data,
            queryPath = queryPath,
            flatten = flatten,
        )

        // So, I actually tested and using a Sequence here is somehow significantly slower
        // So let's stick with the good ol for loop
        val results = mutableListOf<JsonNode>()
        iterator.forEach { (elementQueryPath, elementResultPath, element) ->
            if (elementQueryPath.size == queryPath.segments.size) {
                results.add(JsonNode(element))
            }
        }

        return results
    }
}

/**
 * A JSON node [value] with [queryPath] and [resultPath] values.
 *
 * You should not store the [NadelEphemeralJsonNode] as there is only one instance.
 * Its values will change, but the enclosing [NadelEphemeralJsonNode] instance iis the same.
 *
 * The values should not be stored either.
 *
 * This exists because of performance reasons, we avoid object allocations and reuse
 * the same [List] values to avoid creating multiple arrays.
 *
 * The majority of the time the [resultPath] values are discarded anyway.
 */
internal abstract class NadelEphemeralJsonNode {
    abstract val queryPath: List<String>
    abstract val resultPath: NadelResultPath
    abstract val value: Any?

    companion object {
        operator fun NadelEphemeralJsonNode.component1(): List<String> = queryPath
        operator fun NadelEphemeralJsonNode.component2(): NadelResultPath = resultPath
        operator fun NadelEphemeralJsonNode.component3(): Any? = value
    }
}

/**
 * Does a DFS search through the response to the given `queryPath`.
 */
internal class NadelJsonNodeIterator(
    root: Any?,
    queryPath: NadelQueryPath,
    private val flatten: Boolean,
) : Iterator<NadelEphemeralJsonNode> {
    private var hasNext = true

    override fun hasNext(): Boolean {
        return hasNext || calculateNext()
    }

    override fun next(): NadelEphemeralJsonNode {
        if (!hasNext && !calculateNext()) {
            throw NoSuchElementException()
        }

        hasNext = false
        ephemeralJsonNode.value = parents.last()

        return ephemeralJsonNode
    }

    private val queryPathSegments = queryPath.segments
    private val currentQueryPathSegments = ArrayList<String>(queryPathSegments.size)
    private val currentResultPathSegments = ArrayList<NadelResultPathSegment>(queryPathSegments.size + resultBuffer)

    companion object {
        /**
         * A random guess at a ceiling of how many indices a result path should have over the query path.
         *
         * e.g. query path could be [issues, users, next, friends, enemies] and a result path
         * could be [issues, 0, users, 10, next, friends, 2, enemies, 5]
         *
         * So in this case our result path has 4 more elements than the query path.
         *
         * We use this buffer value to create a "right sized" [List] for storing the result path etc.
         */
        private const val resultBuffer = 6

        private val NONE = Any()
    }

    private val ephemeralJsonNode = object : NadelEphemeralJsonNode() {
        override val queryPath get() = currentQueryPathSegments
        override val resultPath get() = NadelResultPath(currentResultPathSegments)
        override var value: Any? = NONE
    }

    /**
     * These are the parents of the current element, and includes the current element
     * at the end of a traversal iteration.
     */
    private val parents: MutableList<Any?> = ArrayList<Any?>(queryPathSegments.size + resultBuffer).also {
        it.add(root)
    }

    private val objectPathSegmentCache = queryPathSegments.associateWith(NadelResultPathSegment::Object)

    private fun calculateNext(): Boolean {
        val advanced: Boolean = when (val current = parents.last()) {
            is AnyList -> {
                if (currentQueryPathSegments.size == queryPathSegments.size && !flatten) {
                    // Shortcut to avoid traversing children at all if not asked for
                    false
                } else {
                    if (current.isEmpty()) {
                        false
                    } else {
                        if (currentResultPathSegments.lastIndex < parents.lastIndex) {
                            // Traverse children
                            currentResultPathSegments.add(NadelResultPathSegment.Array(current.lastIndex))
                        }

                        val arraySegment = currentResultPathSegments[parents.lastIndex] as NadelResultPathSegment.Array
                        parents.add(current[arraySegment.index])
                        true
                    }
                }
            }
            is AnyMap -> {
                if (currentQueryPathSegments.size < queryPathSegments.size) {
                    val nextQueryPathSegment = queryPathSegments[currentQueryPathSegments.lastIndex + 1]
                    val nextElement = current.getOrDefault(nextQueryPathSegment, NONE)
                    if (nextElement === NONE) {
                        false
                    } else {
                        currentQueryPathSegments.add(nextQueryPathSegment)
                        currentResultPathSegments.add(objectPathSegmentCache[nextQueryPathSegment]!!)
                        parents.add(nextElement)
                        true
                    }
                } else {
                    false
                }
            }
            else -> {
                false
            }
        }

        if (!advanced) {
            while (currentResultPathSegments.isNotEmpty()) {
                val last = currentResultPathSegments.lastOrNull() ?: break

                when (last) {
                    is NadelResultPathSegment.Array -> {
                        if (last.index == 0) {
                            // Nothing more to visit in the array, remember that we traverse end -> front
                            currentResultPathSegments.removeLast()
                            parents.removeLast()
                        } else {
                            // We're moving to the next element
                            currentResultPathSegments[currentResultPathSegments.lastIndex] =
                                NadelResultPathSegment.Array(last.index - 1)
                            parents.removeLast()
                            // Iterate to next element
                            return calculateNext()
                        }
                    }
                    is NadelResultPathSegment.Object -> {
                        currentResultPathSegments.removeLast()
                        currentQueryPathSegments.removeLast()
                        parents.removeLast()
                    }
                }
            }
        }

        hasNext = currentResultPathSegments.isNotEmpty()
        return hasNext
    }
}
