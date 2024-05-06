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

        fun printState(): String {
            return """
                Current query path: ${
                currentQueryPathSegments.joinToString(
                    separator = ",",
                    prefix = "[",
                    postfix = "]"
                )
            }
                Current result path: ${
                currentResultPathSegments.joinToString(
                    separator = ",",
                    prefix = "[",
                    postfix = "]"
                )
            }
            """.replaceIndent(' '.toString().repeat(n = 4))
        }

        println(
            "Trying to find $node from ${
                queryPath.segments.joinToString(
                    separator = ",",
                    prefix = "[",
                    postfix = "]"
                )
            }"
        )

        while (queue.isNotEmpty()) {
            val element = queue.removeLast()
            println("Visiting $element")
            if (element === node.value) {
                println("Got em $currentResultPathSegments")
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
                            NavigationOutcome.DeadEnd // todo: tests
                        } else {
                            queue.add(nextElement)
                            currentResultPathSegments.add(NadelResultPathSegment.Object(nextQueryPathSegment))
                            currentQueryPathSegments.add(nextQueryPathSegment)
                            NavigationOutcome.QueuedChild
                        }
                    } else {
                        NavigationOutcome.DeadEnd  // todo: tests
                    }
                }
                else -> NavigationOutcome.DeadEnd  // todo: tests
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
                                        println("Popping dead end array")
                                        currentResultPathSegments.removeLast()
                                    } else {
                                        println("Moving to next array element ${last.index - 1}")
                                        // We're moving to the next element
                                        currentResultPathSegments[currentResultPathSegments.lastIndex] =
                                            NadelResultPathSegment.Array(last.index - 1)
                                        // Nothing further to fix, stop
                                        break
                                    }
                                }
                                is NadelResultPathSegment.Object -> {
                                    println("Popping up dead end object")
                                    currentResultPathSegments.removeLast()
                                    currentQueryPathSegments.removeLast()
                                }
                            }
                        }
                    }
                }
            }

            println("Loop end\n${printState()}")
        }

        println("Didn't get em")
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
