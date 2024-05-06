package graphql.nadel.result

internal sealed interface NadelResultPathSegment {
    @JvmInline
    value class Object(val key: String) : NadelResultPathSegment

    @JvmInline
    value class Array(val index: Int) : NadelResultPathSegment
}
