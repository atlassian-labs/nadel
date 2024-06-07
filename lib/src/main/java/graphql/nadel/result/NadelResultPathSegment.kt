package graphql.nadel.result

internal sealed interface NadelResultPathSegment {
    @JvmInline
    value class Object(val key: String) : NadelResultPathSegment

    @JvmInline
    value class Array private constructor(val index: Int) : NadelResultPathSegment {
        companion object {
            private val cache = List(2000, ::Array)

            operator fun invoke(index: Int): Array {
                return cache.getOrNull(index) ?: Array(index)
            }
        }
    }
}
