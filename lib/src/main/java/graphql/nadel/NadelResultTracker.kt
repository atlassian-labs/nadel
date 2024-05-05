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
internal class NadelResultTracker {
    private val result = CompletableDeferred<ExecutionResult>()

    suspend fun getResultPath(
        queryPath: NadelQueryPath,
        node: JsonNode,
    ): List<NadelResultPathSegment>? {
        val result = result.await()
        val data = result.getData<Any?>()

        val queryPathSegments = queryPath.segments
        val currentResultPathSegments = mutableListOf<NadelResultPathSegment>()
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

        val queue = mutableListOf<Any?>(data)

        while (queue.isNotEmpty()) {
            val element = queue.removeLast()
            if (element === node.value) {
                return currentResultPathSegments
            }

            when (element) {
                is AnyList -> {
                    if (element.isNotEmpty()) {
                        queue.addAll(element)
                        currentResultPathSegments.add(NadelResultPathSegment.Array(element.lastIndex))
                        continue
                    }
                }
                is AnyMap -> {
                    if (currentQueryPathSegments.size < queryPathSegments.size) {
                        val nextQueryPathSegment = queryPathSegments[currentQueryPathSegments.size]
                        val nextElement = element[nextQueryPathSegment]

                        queue.add(nextElement)
                        currentResultPathSegments.add(NadelResultPathSegment.Object(nextQueryPathSegment))
                        currentQueryPathSegments.add(nextQueryPathSegment)

                        if (nextElement === node.value) {
                            return currentResultPathSegments
                        }
                    }
                }
            }

            // Correct the result path segment i.e. move to the next array element
            var last = currentResultPathSegments.lastOrNull()
            while (last is NadelResultPathSegment.Array) {
                if (last.index == 0) {
                    val removedResultPathSegment = currentResultPathSegments.removeLast()
                    if (removedResultPathSegment is NadelResultPathSegment.Object) {
                        currentQueryPathSegments.removeLast()
                    }
                } else {
                    // We're moving to the next element
                    currentResultPathSegments[currentResultPathSegments.lastIndex] =
                        NadelResultPathSegment.Array(last.index - 1)
                    // Nothing further to fix, stop
                    break
                }

                last = currentResultPathSegments.lastOrNull()
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
