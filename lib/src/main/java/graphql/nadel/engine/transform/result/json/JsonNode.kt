package graphql.nadel.engine.transform.result.json

import graphql.nadel.engine.util.AnyList
import graphql.nadel.engine.util.AnyMap

/**
 * todo: one day split this into a sealed class that acts like a union type for each possible type
 *
 * This union type will give us better type safety
 */
data class JsonNode(val value: Any?) {
    init {
        require(value == null || value is AnyMap || value is AnyList || value is Number || value is Boolean || value is String)
    }
}
