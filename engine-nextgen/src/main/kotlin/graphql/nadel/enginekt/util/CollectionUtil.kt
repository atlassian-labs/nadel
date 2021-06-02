package graphql.nadel.enginekt.util

import java.util.Collections
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

/**
 * Like [singleOrNull] but the single item must be of type [T].
 */
inline fun <reified T> Collection<*>.singleOfTypeOrNull(predicate: (T) -> Boolean = { true }): T? {
    return singleOrNull { it is T && predicate(it) } as? T
}

/**
 * Like [singleOrNull] but the single item must be of type [T].
 */
inline fun <reified T> Collection<*>.singleOfType(predicate: (T) -> Boolean = { true }): T {
    return singleOfTypeOrNull(predicate) as T
}

inline fun <K, E> Iterable<E>.strictAssociateBy(crossinline keyExtractor: (E) -> K): Map<K, E> {
    return mapFrom(
        map {
            keyExtractor(it) to it
        }
    )
}

inline fun <K, E> Sequence<E>.strictAssociateBy(crossinline keyExtractor: (E) -> K): Map<K, E> {
    val map = mutableMapOf<K, E>()
    var count = 0
    forEach {
        map[keyExtractor(it)] = it
        count++
    }
    require(map.size == count)
    return Collections.unmodifiableMap(map)
}

/**
 * Like [mapOf] but takes in a [Collection] instead of vararg. Useful if your input
 * into the [Map] is [List.map]ped from another [Collection].
 */
@JvmName("mapFromPairs")
fun <K, V> mapFrom(entries: Collection<Pair<K, V>>): Map<K, V> {
    val map = HashMap<K, V>(entries.size)
    map.putAll(entries)
    require(map.size == entries.size)
    return map
}

/**
 * Like [mapOf] but takes in a [Collection] instead of vararg. Useful if your input
 * into the [Map] is [List.map]ped from another [Collection].
 */
@JvmName("mapFromEntries")
fun <K, V> mapFrom(entries: Collection<Map.Entry<K, V>>): Map<K, V> {
    val map = HashMap<K, V>(entries.size)
    entries.forEach(map::put)
    require(map.size == entries.size)
    return map
}

/**
 * Utility function to set the [Map.Entry.key] to [Map.Entry.value] in the given [MutableMap].
 */
fun <K, V> MutableMap<K, V>.put(entry: Map.Entry<K, V>) {
    put(entry.key, entry.value)
}

/**
 * Utility function to set the [Map.Entry.key] to [Map.Entry.value] in the given [MutableMap].
 */
fun <K, V> MutableMap<K, V>.put(entry: Pair<K, V>) {
    put(entry.first, entry.second)
}

/**
 * Inverts the [Map] such that the values are now the keys and the keys are now the values.
 */
fun <K, V> Map<K, V>.invert(): Map<V, K> {
    val map = HashMap<V, K>(this.size)
    forEach { (key, value) ->
        map[value] = key
    }
    require(map.size == this.size)
    return map
}

/**
 * Try to replace the function body with just:
 *
 * ```kotlin
 *putAll(map)
 * ```
 *
 * If it works then yay we don't need this function anymore, but right now I'm getting:
 *
 * Type mismatch
 *
 * Required: Map<Nothing, Nothing>
 *
 * Found:    Map<*, *>
 */
fun AnyMutableMap.hackPutAll(map: AnyMap) {
    @Suppress("UNCHECKED_CAST")
    (this as MutableMap<Any?, Any?>).putAll(map)
}

fun <T : Any> Iterable<T>.emptyOrSingle(): T? {
    return when (this) {
        is List<T> -> when (isEmpty()) {
            true -> null
            else -> single()
        }
        else -> iterator().emptyOrSingle()
    }
}

fun <T : Any> Iterator<T>.emptyOrSingle(): T? {
    return when (hasNext()) {
        true -> next().also {
            if (hasNext()) {
                // Copied from List.single
                throw IllegalArgumentException("List has more than one element.")
            }
        }
        else -> null
    }
}

fun <T : Any> Sequence<T>.emptyOrSingle(): T? {
    return iterator().emptyOrSingle()
}

inline fun <K, reified T> Map<K, *>.filterValuesOfType(): Map<K, T> {
    @Suppress("UNCHECKED_CAST")
    return filterValues {
        it is T
    } as Map<K, T>
}

inline fun <I, T> Iterable<I>.mapToArrayList(
    destination: ArrayList<T> = ArrayList(),
    transform: (I) -> T,
): ArrayList<T> {
    return mapTo(destination, transform)
}

fun Sequence<Any?>.flatten(recursively: Boolean): Sequence<Any?> {
    return flatMap { element ->
        when (element) {
            is AnyIterable -> element.asSequence().let {
                if (recursively) {
                    it.flatten(recursively = true)
                } else {
                    it
                }
            }
            else -> sequenceOf(element)
        }
    }
}
