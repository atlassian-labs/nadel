package graphql.nadel.engine.blueprint.hydration

import graphql.nadel.engine.transform.query.NadelQueryPath

sealed class NadelHydrationCondition {
    abstract val fieldPath: NadelQueryPath

    abstract fun evaluate(fieldValue: Any?): Boolean

    data class StringResultEquals(
        override val fieldPath: NadelQueryPath,
        val value: String,
    ) : NadelHydrationCondition() {
        override fun evaluate(fieldValue: Any?): Boolean {
            if (fieldValue is String) {
                return fieldValue == value
            }
            return false
        }
    }

    data class LongResultEquals(
        override val fieldPath: NadelQueryPath,
        val value: Long,
    ) : NadelHydrationCondition() {
        override fun evaluate(fieldValue: Any?): Boolean {
            if (fieldValue is Int) {
                return fieldValue.toLong() == value
            }
            if (fieldValue is Long) {
                return fieldValue == value
            }
            return false
        }
    }

    data class StringResultMatches(
        override val fieldPath: NadelQueryPath,
        val regex: Regex,
    ) : NadelHydrationCondition() {
        override fun evaluate(fieldValue: Any?): Boolean {
            if (fieldValue is String) {
                return fieldValue.matches(regex)
            }
            return false
        }
    }

    data class StringResultStartsWith(
        override val fieldPath: NadelQueryPath,
        val prefix: String,
    ) : NadelHydrationCondition() {
        override fun evaluate(fieldValue: Any?): Boolean {
            if (fieldValue is String) {
                return fieldValue.startsWith(prefix)
            }
            return false
        }
    }
}
