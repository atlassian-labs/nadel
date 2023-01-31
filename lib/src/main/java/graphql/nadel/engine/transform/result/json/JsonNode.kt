package graphql.nadel.engine.transform.result.json

import graphql.nadel.engine.util.AnyList
import graphql.nadel.engine.util.AnyMap

@JvmInline
value class JsonNode(val value: Any?) {
    init {
        require(value == null || value is AnyMap || value is AnyList || value is Number || value is Boolean || value is String)
    }
}
