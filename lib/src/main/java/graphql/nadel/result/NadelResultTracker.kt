package graphql.nadel.result

import graphql.ExecutionResult
import graphql.nadel.engine.transform.query.NadelQueryPath
import graphql.nadel.engine.transform.result.json.JsonNode
import graphql.nadel.engine.transform.result.json.NadelJsonNodeIterator
import kotlinx.coroutines.CompletableDeferred

/**
 * todo: this needs to track multiple responses
 */
internal class NadelResultTracker {
    private val result = CompletableDeferred<ExecutionResult>()

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
    ): NadelResultPath? {
        val result = result.await()
        val data = result.toSpecification()["data"]

        println()
        println("Looking at $queryPath for $node")

        val jsonNodeIterator = NadelJsonNodeIterator(root = data, queryPath = queryPath, flatten = true)
        for (ephemeralNode in jsonNodeIterator) {
            println("Traversing node\n\tQuery path: ${ephemeralNode.queryPath}\n\tResult path: ${ephemeralNode.resultPath}\n\tValue: ${ephemeralNode.value}")
            if (ephemeralNode.queryPath.size == queryPath.segments.size && ephemeralNode.value === node.value) {
                // Clone because underlying values are ephemeral too
                return ephemeralNode.resultPath.clone()
            }
        }

        println()
        return null
    }

    fun complete(value: ExecutionResult) {
        result.complete(value)
    }
}
