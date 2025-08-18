package graphql.nadel.tests.util

import io.kotest.core.spec.style.DescribeSpec
import strikt.api.expectThat
import strikt.assertions.isEqualTo

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
        .replace(Regex("^([^A-Za-z0-9]+)?(.+?)([^A-Za-z0-9]+)?$")) { // Remove preceding or trailing characters
            it.groupValues[2]
        }
        .replace(Regex("[a-z][A-Z]+")) { // Handles camelCase
            it.value.first() + " " + it.value.drop(1).toCharArray().joinToString(separator = " ")
        }
        .replace(Regex("-{2,}"), "-") // Removes consecutive dashes
        .replaceAll(Regex(".[-_].")) { // Handles kebab-case and snake_case
            it.value.first() + " " + it.value.last()
        }
        .replace(Regex(".\\.")) { // Handles dot
            it.value.first() + " "
        }
        .replace(Regex("\\s{2,}")) { // Consecutive spaces
            " "
        }
        .trim()
        .lowercase()
}

/**
 * This is probably pretty inefficient but hey, it's only used in tests.
 */
private inline fun String.replaceAll(regex: Regex, crossinline transform: (MatchResult) -> CharSequence): String {
    var string = this
    do {
        var modCount = 0
        string = string.replace(regex) {
            modCount++
            transform(it)
        }
    } while (modCount != 0)
    return string
}

class WordCasingTest : DescribeSpec({
    describe("to no case") {
        it("strips characters") {
            // Given
            val tests = listOf(
                "thisIsAString", // camel
                "THIS_IS_A_STRING", // constant
                "this.is.a.string", // dot
                "ThisIsAString", // pascal
                "This is a string.", // sentence
                "this_is_a_string", // snake
                "this is a string", // space
                "--this-is-a-string", // consecutive dashes
                "_this-is-a-string", // trailing characters
                "___this-is-a-string", // trailing characters
                "___this-is-a-string_-_", // trailing characters
                "this-is-a-string--", // trailing characters
                "this-is-a-string-", // trailing characters
                "this-is-a-string__", // trailing characters
                "this-is-a-string___", // trailing characters
            )
            tests.forEach { input ->
                expectThat(input)
                    .get {
                        input.toNoCase()
                    }
                    .isEqualTo("this is a string")
            }
        }
    }
})
