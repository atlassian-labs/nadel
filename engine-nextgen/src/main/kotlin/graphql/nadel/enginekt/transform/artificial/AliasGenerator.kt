package graphql.nadel.enginekt.transform.artificial

import graphql.normalized.ExecutableNormalizedField
import java.util.UUID

object AliasGenerator {
    /**
     * Random substrings to attach to the field alias to make it unique enough such
     * that nobody should ever guess the field result key and mess with the fields
     * introduced by Nadel.
     */
    private val suffixes = Array(100) {
        UUID.randomUUID().toString().substringAfterLast("-")
    }

    /**
     * Determines whether the aliases generated should be static.
     *
     * If `false` then a random identifier is stuck onto the end of the alias.
     */
    var isStatic: Boolean = false

    fun getAlias(tag: String, field: ExecutableNormalizedField): String {
        val staticAlias = getStaticAlias(tag, field)
        if (isStatic) {
            return staticAlias
        }
        /**
         * Note that even without the suffix the alias here is unique among its siblings fields.
         * See [suffixes] for more.
         */
        return "${staticAlias}__${suffixes.random()}"
    }

    /**
     * Note that this is static alias by itself is enough for uniqueness among sibling fields.
     */
    private fun getStaticAlias(tag: String, field: ExecutableNormalizedField): String {
        return "${tag}__${field.resultKey}"
    }
}
