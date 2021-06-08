package graphql.nadel.enginekt.transform.query

data class QueryPath(val segments: List<String>) {
    constructor(segment: String) : this(listOf(segment))

    operator fun plus(segment: String): QueryPath {
        return QueryPath(segments + segment)
    }

    fun dropLast(n: Int): QueryPath {
        return QueryPath(segments.dropLast(n))
    }

    inline fun mapIndexed(mapper: (index: Int, segment: String) -> String): QueryPath {
        return QueryPath(segments.mapIndexed(mapper))
    }

    fun last(): String {
        return segments.last()
    }

    companion object {
        val root = QueryPath(emptyList())
    }
}
