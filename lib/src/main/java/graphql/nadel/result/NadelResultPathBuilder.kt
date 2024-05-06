package graphql.nadel.result

internal interface NadelResultPathBuilder {
    fun add(key: String): NadelResultPathBuilder
    fun add(index: Int): NadelResultPathBuilder
    fun build(): List<NadelResultPathSegment>

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

                override fun build(): List<NadelResultPathSegment> {
                    return segments
                }
            }
        }
    }
}

internal fun List<NadelResultPathSegment>.toBuilder(): NadelResultPathBuilder {
    return NadelResultPathBuilder(this)
}
