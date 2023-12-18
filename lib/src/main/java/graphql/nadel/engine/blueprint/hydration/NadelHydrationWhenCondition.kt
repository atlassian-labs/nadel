package graphql.nadel.engine.blueprint.hydration

import graphql.nadel.engine.transform.query.NadelQueryPath
sealed class NadelHydrationWhenCondition {
    abstract fun evaluate(resultId: String): Boolean

    data class ResultEquals(
        val fieldPath: NadelQueryPath,
        val value: Any,
    ): NadelHydrationWhenCondition() {
        override fun evaluate(resultId: String): Boolean {
            TODO("Not yet implemented")
        }
    }
    data class StringResultMatches(
        val fieldPath: NadelQueryPath,
        val regex: Regex,
    ): NadelHydrationWhenCondition() {
        override fun evaluate(resultId: String): Boolean {
            TODO("Not yet implemented")
        }
    }

    data class StringResultStartsWith(
        val fieldPath: NadelQueryPath,
        val value: String,
    ): NadelHydrationWhenCondition() {
        override fun evaluate(resultId: String): Boolean {
            TODO("Not yet implemented")
        }
    }
}