package graphql.nadel.tests.util

fun join(vararg strings: String, separator: String): String {
    return sequenceOf(*strings).joinToString(separator)
}

fun String.toSlug(): String {
    return toNoCase().replace(" ", "-")
}

/**
 * Loosely adapted from https://github.com/ianstormtaylor/to-no-case/blob/26075f487c13a04d2dcdb7c424b106c39fbaae3a/index.js
 */
fun String.toNoCase(): String {
    return this
        .replace(Regex("[a-z][A-Z]+")) { // Handles camelCase
            it.value.first() + " " + it.value.drop(1).toCharArray().joinToString(separator = " ")
        }
        .replace(Regex("-{2,}"), "-") // Removes consecutive dashes
        .replace(Regex(".[-_].")) { // Handles kebab-case and snake_case
            it.value.first() + " " + it.value.last()
        }
        .replace(Regex(".\\.")) { // Handles dot
            it.value.first() + " "
        }
        .replace(Regex("\\s{2,}")) { // Consecutive spaces
            " "
        }
        .trim()
        .toLowerCase()
}
