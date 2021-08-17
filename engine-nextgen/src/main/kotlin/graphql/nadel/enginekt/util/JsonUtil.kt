package graphql.nadel.enginekt.util

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

internal fun recursivelyRemoveKey(map: MutableJsonMap?, key: String) {
    if (map == null) {
        return
    }
    map.remove(key)
    map.keys.forEach {
        val value = map[it]
        if (value is Map<*, *>) {
            recursivelyRemoveKey(value.asMutableJsonMap(), key)
        } else if (value is List<*>) {
            recursivelyRemoveKey(value, key)
        }
    }
}

private fun recursivelyRemoveKey(list: List<*>, key: String) {
    list.forEach {
        if (it is List<*>) {
            recursivelyRemoveKey(it, key)
        } else if (it is Map<*, *>) {
            recursivelyRemoveKey(it.asMutableJsonMap(), key)
        }
    }
}
