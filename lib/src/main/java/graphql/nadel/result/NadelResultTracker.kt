package graphql.nadel.result

import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.AnyList
import graphql.nadel.engine.util.AnyMap
import kotlinx.coroutines.CompletableDeferred

/**
 * todo: this needs to track multiple responses
 */
internal class NadelResultTracker {
    private val result = CompletableDeferred<ExecutionResult>()

    private enum class NavigationOutcome {
        QueuedChild,
        ExpandedArray,
        DeadEnd,
    }

    /**
     * So… in Nadel the result can change a lot.
     *
     * This function lets you track where a result node went to in the overall response sent to the user.
     *
     * I haven't benchmarked this yet, but in theory it should be more performant than say using
     * [graphql.nadel.engine.transform.result.json.JsonNodes].
     *
     * In the past we used to track the [NadelResultPathSegment]s for each [JsonNode] but that was horrible
     * performance wise because we created one List for each result node
     * i.e. as the result grew, both in depth and result node count, you'd allocate tons of (big) lists.
     *
     * This implementation keeps track of the _current_ [NadelResultPathSegment]s and returns that if it
     * finds the [node] in question.
     */
    suspend fun getResultPath(
        queryPath: NadelQueryPath,
        node: JsonNode,
    ): List<NadelResultPathSegment>? {
        val result = result.await()
        val data = result.getData<Any?>()

        val queryPathSegments = queryPath.segments
        val currentResultPathSegments = mutableListOf<NadelResultPathSegment>()
        val currentQueryPathSegments = mutableListOf<String>()

        val queue = mutableListOf<Any?>(data)

        while (queue.isNotEmpty()) {
            val element = queue.removeLast()
            if (element === node.value) {
                return currentResultPathSegments
            }

            val outcome: NavigationOutcome = when (element) {
                is AnyList -> {
                    if (element.isNotEmpty()) {
                        queue.addAll(element)
                        currentResultPathSegments.add(NadelResultPathSegment.Array(element.lastIndex))
                        NavigationOutcome.ExpandedArray
                    } else {
                        NavigationOutcome.DeadEnd
                    }
                }
                is AnyMap -> {
                    if (currentQueryPathSegments.size < queryPathSegments.size) {
                        val nextQueryPathSegment = queryPathSegments[currentQueryPathSegments.size]
                        val nextElement = element[nextQueryPathSegment]

                        if (nextElement == null) {
                            NavigationOutcome.DeadEnd
                        } else {
                            queue.add(nextElement)
                            currentResultPathSegments.add(NadelResultPathSegment.Object(nextQueryPathSegment))
                            currentQueryPathSegments.add(nextQueryPathSegment)
                            NavigationOutcome.QueuedChild
                        }
                    } else {
                        NavigationOutcome.DeadEnd
                    }
                }
                else -> NavigationOutcome.DeadEnd
            }

            when (outcome) {
                NavigationOutcome.QueuedChild -> {
                }
                NavigationOutcome.ExpandedArray -> {
                }
                NavigationOutcome.DeadEnd -> {
                    if (queue.isNotEmpty()) { // i.e. we have other array elements to visit
                        while (currentResultPathSegments.isNotEmpty()) {
                            val last = currentResultPathSegments.lastOrNull() ?: break

                            when (last) {
                                is NadelResultPathSegment.Array -> {
                                    if (last.index == 0) {
                                        // Nothing more to visit in the array, remember that we traverse end -> front
                                        currentResultPathSegments.removeLast()
                                    } else {
                                        // We're moving to the next element
                                        currentResultPathSegments[currentResultPathSegments.lastIndex] =
                                            NadelResultPathSegment.Array(last.index - 1)
                                        // Nothing further to fix, stop
                                        break
                                    }
                                }
                                is NadelResultPathSegment.Object -> {
                                    currentResultPathSegments.removeLast()
                                    currentQueryPathSegments.removeLast()
                                }
                            }
                        }
                    }
                }
            }
        }

        return null
    }

    fun complete(value: ExecutionResult) {
        result.complete(value)
    }

    fun complete(value: DelayedIncrementalPartialResult) {
        // result.complete(value)
        // todo: track here
    }
}
