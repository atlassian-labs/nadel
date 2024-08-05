package graphql.nadel.result

@JvmInline
internal value class NadelResultPath(
    val value: List<NadelResultPathSegment>,
) {
    operator fun plus(key: String): NadelResultPath {
        return NadelResultPath(value + NadelResultPathSegment.Object(key))
    }

    operator fun plus(key: Int): NadelResultPath {
        return NadelResultPath(value + NadelResultPathSegment.Array(key))
    }

    fun clone(): NadelResultPath {
        return NadelResultPath(value = value.toList())
    }

    fun toRawPath(): List<Any> {
        return value.map {
            when (it) {
                is NadelResultPathSegment.Array -> it.index
                is NadelResultPathSegment.Object -> it.key
            }
        }
    }

    companion object {
        val empty = NadelResultPath(value = emptyList())
    }
}
