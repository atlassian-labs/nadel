package graphql.nadel.tests.util

import java.text.Normalizer
import java.util.Locale
import java.util.regex.Pattern

fun join(vararg strings: String, separator: String): String {
    return sequenceOf(*strings).joinToString(separator)
}

fun String.toSlug(): String {
    val noWhitespace = Slug.WHITESPACE.matcher(this).replaceAll("-")
    val normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD)
    val slug = Slug.NON_LATIN.matcher(normalized).replaceAll("")
    return slug.toLowerCase(Locale.ENGLISH)
}

private object Slug {
    val NON_LATIN: Pattern = Pattern.compile("[^\\w-]")
    val WHITESPACE: Pattern = Pattern.compile("[\\s]")
}
