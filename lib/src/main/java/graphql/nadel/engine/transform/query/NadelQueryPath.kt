package graphql.nadel.engine.transform.query

data class NadelQueryPath(val segments: List<String>) {
    constructor(segment: String) : this(listOf(segment))

    val size: Int get() = segments.size

    operator fun plus(segment: String): NadelQueryPath {
        return NadelQueryPath(segments + segment)
    }

    fun dropLast(n: Int): NadelQueryPath {
        return NadelQueryPath(segments.dropLast(n))
    }

    inline fun mapIndexed(mapper: (index: Int, segment: String) -> String): NadelQueryPath {
        return NadelQueryPath(segments.mapIndexed(mapper))
    }

    fun last(): String {
        return segments.last()
    }

    companion object {
        val root = NadelQueryPath(emptyList())
    }
}
