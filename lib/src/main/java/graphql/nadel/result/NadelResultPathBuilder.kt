package graphql.nadel.result

internal interface NadelResultPathBuilder {
    fun add(key: String): NadelResultPathBuilder
    fun add(index: Int): NadelResultPathBuilder
    fun build(): NadelResultPath

    companion object {
        operator fun invoke(
            path: List<NadelResultPathSegment> = emptyList(),
        ): NadelResultPathBuilder {
            return object : NadelResultPathBuilder {
                private var segments = path.toMutableList()

                override fun add(key: String): NadelResultPathBuilder {
                    segments.add(NadelResultPathSegment.Object(key))
                    return this
                }

                override fun add(index: Int): NadelResultPathBuilder {
                    segments.add(NadelResultPathSegment.Array(index))
                    return this
                }

                override fun build(): NadelResultPath {
                    return NadelResultPath(segments)
                }
            }
        }
    }
}

internal fun NadelResultPath.toBuilder(): NadelResultPathBuilder {
    return NadelResultPathBuilder(value)
}

internal fun List<NadelResultPathSegment>.toBuilder(): NadelResultPathBuilder {
    return NadelResultPathBuilder(this)
}
