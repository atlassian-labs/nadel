package graphql.nadel.engine.transform.query

import graphql.Assert.assertShouldNeverHappen

data class NadelQueryPath(val segments: List<String>) {
    val size: Int get() = segments.size

    operator fun plus(segment: String): NadelQueryPath {
        return NadelQueryPath(segments + segment)
    }

    fun drop(n: Int): NadelQueryPath {
        return NadelQueryPath(segments.drop(n))
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

    fun startsWith(prefix: List<String>): Boolean {
        if (prefix.size > segments.size) return false
        for (i in 0..<prefix.size) {
            if (prefix[i] != segments[i]) return false
        }
        return true
    }

    fun removePrefix(prefix: List<String>): NadelQueryPath {
        if (this.startsWith(prefix)) {
            return NadelQueryPath(segments.drop(prefix.size))
        }
        return assertShouldNeverHappen("NadelQueryPath did not start with prefix")
    }

    companion object {
        val root = NadelQueryPath(emptyList())

        fun fromResultPath(path: List<Any>): NadelQueryPath {
            return NadelQueryPath(path.filterIsInstance<String>())
        }
    }
}
