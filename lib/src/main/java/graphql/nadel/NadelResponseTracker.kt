package graphql.nadel

import graphql.ExecutionResult
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.util.AnyList
import graphql.nadel.engine.util.AnyMap
import kotlinx.coroutines.CompletableDeferred

internal sealed interface NadelResultPathSegment {
    @JvmInline
    value class Object(val key: String) : NadelResultPathSegment

    @JvmInline
    value class Array(val index: Int) : NadelResultPathSegment
}

/**
 * todo: as delayed responses go out we need to track what objects went out
 */
/**
 * note: a Flow<Result> will mean that we can GC the original result sooner
 */
class ResultListener {
    fun track(delayed: DelayedIncrementalPartialResult) {
        // todo: track objects in here
    }
}

/**
 * todo: this needs to track multiple responses
 */
internal class NadelResponseTracker {
    private val result = CompletableDeferred<ExecutionResult>()

    // todo: maybe store some kind of Map<IdentityHashCode, ResultPath> to free up memory earlier?

    suspend fun getResponsePath(
        queryPath: NadelQueryPath,
        node: JsonNode,
    ): List<NadelResultPathSegment>? {
        val result = result.await()
        val data = result.getData<Any?>()

        val queryPathSegments = queryPath.segments
        val currentResultPath = mutableListOf<NadelResultPathSegment>()
        val currentQueryPathSegments = mutableListOf<String>()

        /**
         * {
         *   "data": {
         *      users: [
         *          {
         *              friends: []
         *          }
         *      ]
         *   }
         * }
         */

        // todo: how do we know if we need to pop?
        val queue = mutableListOf<Any?>(data)

        // users[0].friend.user[1]
        // [a, b, c]
        // b
        // [a] <-- if we hit this then we should decrement the index

        fun printState(): String? {
            return """
                Current query path: ${
                currentQueryPathSegments.joinToString(
                    separator = ",",
                    prefix = "[",
                    postfix = "]"
                )
            }
                Current result path: ${currentResultPath.joinToString(separator = ",", prefix = "[", postfix = "]")}
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
            if (element === node.value) {
                println("Got em $currentResultPath")
                return currentResultPath
            }

            when (element) {
                is AnyList -> {
                    // todo: expand the queue here
                    if (element.isNotEmpty()) {
                        queue.addAll(element)
                        currentResultPath.add(NadelResultPathSegment.Array(element.lastIndex)) // DFS so we traverse last element first
                        continue
                    }
                }
                is AnyMap -> {
                    if (currentQueryPathSegments.size < queryPathSegments.size) {
                        val nextQueryPathSegment = queryPathSegments[currentQueryPathSegments.size]
                        println("Entering $nextQueryPathSegment\n${printState()}")
                        val nextElement = element[nextQueryPathSegment]

                        queue.add(nextElement)
                        currentResultPath.add(NadelResultPathSegment.Object(nextQueryPathSegment))
                        currentQueryPathSegments.add(nextQueryPathSegment)

                        if (nextElement === node.value) {
                            println("Got em on next $currentResultPath")
                            return currentResultPath
                        }
                    }
                }
            }

            println("Loop end")

            // Correct the result path
            var last = currentResultPath.lastOrNull()
            while (last is NadelResultPathSegment.Array) {
                if (last.index == 0) {
                    println("We're popping up\n${printState()}")
                    currentResultPath.removeLast()
                    currentQueryPathSegments.removeLast() // todo: is this always the case? what about nested lists
                } else {
                    println("Moving to next array element ${last.index - 1}")
                    // We're moving to the next element
                    currentResultPath[currentResultPath.lastIndex] =
                        NadelResultPathSegment.Array(last.index - 1)
                    // Nothing further to fix, stop
                    break
                }

                last = currentResultPath.lastOrNull()
            }
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
