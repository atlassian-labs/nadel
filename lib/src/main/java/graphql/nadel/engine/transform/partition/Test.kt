package graphql.nadel.engine.transform.partition

fun mergeListAtPath(
    originalMap: Map<String, Any>,
    partitionedMaps: List<Map<String, Any>>,
    path: List<String>,
): Map<String, Any>? {

    val immutableMap = originalMap.toMutableMapRecursively()
    var tempOriginal: MutableMap<String, Any> = immutableMap
    var tempPartitioned: List<Map<*, *>> = partitionedMaps

    path.forEachIndexed { idx, value ->
        if (tempOriginal[value] is Map<*, *>) {
            tempOriginal = tempOriginal[value] as MutableMap<String, Any>
            tempPartitioned = tempPartitioned.map { it[value] as Map<*, *> }
        } else if (path.size - 1 == idx && tempOriginal[value] is List<*>) {
            tempOriginal[value] = (tempOriginal[value] as List<*>) + tempPartitioned.flatMap { it[value] as List<*> }
        } else {
            return null
        }
    }

    return immutableMap
}

fun <K, V> Map<K, V>.toMutableMapRecursively(): MutableMap<K, V> {
    val mutableMap = this.toMutableMap()
    for ((key, value) in mutableMap) {
        if (value is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            mutableMap[key] = (value as Map<K, V>).toMutableMapRecursively() as V
        }
    }
    return mutableMap
}

fun main() {
    val map1 = mapOf(
        "a" to mapOf("a.1" to listOf("1", "2")),
        "b" to "value"
    )

    val maps = listOf(
        mapOf(
            "a" to mapOf("a.1" to listOf("3")),
        ),
        mapOf(
            "a" to mapOf("a.1" to listOf("4"))
        )
    )

    val r = mergeListAtPath(map1, maps, listOf("a", "a.1"))

    println(r)
}
