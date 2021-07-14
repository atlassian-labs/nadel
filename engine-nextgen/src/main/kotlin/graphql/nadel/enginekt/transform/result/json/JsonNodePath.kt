package graphql.nadel.enginekt.transform.result.json

typealias AnyJsonNodePathSegment = JsonNodePathSegment<*>

data class JsonNodePath(
    val segments: List<AnyJsonNodePathSegment>,
) {
    operator fun plus(segment: String): JsonNodePath {
        return copy(segments = segments + JsonNodePathSegment.String(segment))
    }

    operator fun plus(segment: Int): JsonNodePath {
        return copy(segments = segments + JsonNodePathSegment.Int(segment))
    }

    operator fun plus(other: JsonNodePath): JsonNodePath {
        return copy(segments = segments + other.segments)
    }

    operator fun plus(other: AnyJsonNodePathSegment): JsonNodePath {
        return copy(segments = segments + other)
    }

    fun dropLast(n: Int): JsonNodePath {
        return copy(segments = segments.dropLast(n))
    }

    companion object {
        val root = JsonNodePath(segments = emptyList())
    }
}

sealed class JsonNodePathSegment<T : Any> {
    abstract val value: T

    data class Int(override val value: kotlin.Int) : JsonNodePathSegment<kotlin.Int>()
    data class String(override val value: kotlin.String) : JsonNodePathSegment<kotlin.String>()

    override fun toString(): kotlin.String {
        return "$value"
    }
}
