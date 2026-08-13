package graphql.nadel.engine.util

internal fun AnyMap.asJsonMap(): JsonMap {
    @Suppress("UNCHECKED_CAST")
    return this as JsonMap
}

internal fun AnyMap.asMutableJsonMap(): MutableJsonMap {
    @Suppress("UNCHECKED_CAST")
    return this as MutableJsonMap
}

internal fun AnyMap?.asNullableJsonMap(): JsonMap? {
    @Suppress("UNCHECKED_CAST")
    return this as JsonMap?
}

/**
 * Hydration results are mutable because later result transforms can rewrite them. Shared
 * hydration fan-out must therefore give each consumer its own object graph.
 */
internal fun deepCopyJsonValue(value: Any?): Any? {
    return when (value) {
        is Map<*, *> -> value.entries.associateTo(LinkedHashMap()) { (key, child) ->
            key as String to deepCopyJsonValue(child)
        }
        is List<*> -> value.mapTo(ArrayList(value.size), ::deepCopyJsonValue)
        else -> value
    }
}
