package graphql.nadel.tests.next

import com.fasterxml.jackson.module.kotlin.convertValue
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.engine.util.MutableJsonMap
import graphql.nadel.tests.jsonObjectMapper

fun joinExecutionResults(result: JsonMap, incrementalResults: List<JsonMap>): JsonMap {
    val deepClone = jsonObjectMapper.convertValue<MutableJsonMap>(result)

    @Suppress("UNCHECKED_CAST")
    val resultData = deepClone["data"] as JsonMap?

    if (resultData != null) {
        incrementalResults
            .map {
                // Deep clone, to avoid mutating the original response
                jsonObjectMapper.convertValue<MutableJsonMap>(it)
            }
            .forEach { incrementalResult ->
                @Suppress("UNCHECKED_CAST")
                val payloads = incrementalResult["incremental"] as? List<JsonMap>
                payloads?.forEach { payload ->
                    @Suppress("UNCHECKED_CAST")
                    val path = payload["path"]!! as List<Any>

                    when {
                        "data" in payload -> {
                            val data = payload["data"]!!
                            setDeferred(resultData, path, data)
                        }
                        "items" in payload -> {
                            throw UnsupportedOperationException()
                        }
                    }
                }
            }
    }

    deepClone.remove("hasNext")
    return deepClone
}

private fun setDeferred(result: JsonMap, path: List<Any>, data: Any) {
    when (val lastPathSegment = path.last()) {
        is Int -> {
            val parent = result.getValueAt(path)
            @Suppress("UNCHECKED_CAST")
            (parent as MutableJsonMap?)?.putAll(data as JsonMap)
        }
        is String -> {
            val parent = result.getValueAt(path.dropLast(n = 1))
            @Suppress("UNCHECKED_CAST")
            (parent as MutableJsonMap?)?.put(lastPathSegment, data)
        }
    }
}

private fun JsonMap.getValueAt(path: List<Any>): Any? {
    return path.foldWhileNotNull<Any, Any>(this) { element, pathSegment ->
        when (pathSegment) {
            is String -> (element as Map<*, *>?)?.get(pathSegment)
            is Int -> (element as List<*>?)?.get(pathSegment)
            else -> throw UnsupportedOperationException()
        }
    }
}

/**
 * Fork of [Iterable.fold] that only continues if the accumulated value is still not null.
 *
 * This adds a shortcut on the for loop on the [Iterable] for performance.
 */
private inline fun <T, R : Any> Iterable<T>.foldWhileNotNull(initial: R?, operation: (acc: R, T) -> R?): R? {
    var accumulator = initial
    for (element in this) {
        accumulator = operation(accumulator ?: return null, element)
    }
    return accumulator
}
