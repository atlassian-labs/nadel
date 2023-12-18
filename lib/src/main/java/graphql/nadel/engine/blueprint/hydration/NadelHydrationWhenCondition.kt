package graphql.nadel.engine.blueprint.hydration

import graphql.nadel.engine.transform.query.NadelQueryPath

sealed class NadelHydrationWhenCondition {
    abstract val fieldPath: NadelQueryPath

    abstract fun evaluate(fieldValue: Any?): Boolean

    data class ResultEquals(
        override val fieldPath: NadelQueryPath,
        val value: Any,
    ) : NadelHydrationWhenCondition() {
        override fun evaluate(fieldValue: Any?): Boolean {
            return fieldValue == value
        }
    }

    data class StringResultMatches(
        override val fieldPath: NadelQueryPath,
        val regex: Regex,
    ) : NadelHydrationWhenCondition() {
        override fun evaluate(fieldValue: Any?): Boolean {
            return (fieldValue as String).matches(regex)
        }
    }

    data class StringResultStartsWith(
        override val fieldPath: NadelQueryPath,
        val prefix: String,
    ) : NadelHydrationWhenCondition() {
        override fun evaluate(fieldValue: Any?): Boolean {
            return (fieldValue as String).startsWith(prefix)
        }
    }
}