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
