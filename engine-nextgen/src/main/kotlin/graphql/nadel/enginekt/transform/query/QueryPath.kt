package graphql.nadel.enginekt.transform.query

data class QueryPath(val segments: List<String>) {

    operator fun plus(segment: String): QueryPath {
        return QueryPath(segments + segment)
    }

    fun dropLast(n: Int): QueryPath {
        return QueryPath(segments.dropLast(n))
    }

    fun last(): String {
        return segments.last()
    }

    companion object {
        val root = QueryPath(emptyList())
    }
}