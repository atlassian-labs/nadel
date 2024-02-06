package graphql.nadel.engine.transform.hydration.batch.indexing

import graphql.nadel.engine.transform.result.json.JsonNode

@Suppress("DataClassPrivateConstructor") // Whatever, no matter
internal data class NadelBatchHydrationIndexKey private constructor(private val key: Any?) {
    constructor(node: JsonNode?) : this(key = node)
    constructor(nodes: List<JsonNode?>) : this(key = nodes)
}
