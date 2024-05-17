package graphql.nadel.tests

import graphql.nadel.engine.util.AnyList
import graphql.nadel.engine.util.JsonMap
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompare
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.JSONCompareResult

fun assertJsonEquals(
    expected: AnyList?,
    actual: AnyList?,
    mode: JSONCompareMode = JSONCompareMode.STRICT,
) {
    assertJsonEquals(
        expected = expected?.let(jsonObjectMapper::writeValueAsString),
        actual = actual?.let(jsonObjectMapper::writeValueAsString),
        mode = mode,
    )
}

fun assertJsonEquals(
    expected: JsonMap?,
    actual: JsonMap?,
    mode: JSONCompareMode = JSONCompareMode.STRICT,
) {
    assertJsonEquals(
        expected = expected?.let(jsonObjectMapper::writeValueAsString),
        actual = actual?.let(jsonObjectMapper::writeValueAsString),
        mode = mode,
    )
}

fun assertJsonEquals(
    expected: String?,
    actual: String?,
    mode: JSONCompareMode = JSONCompareMode.STRICT,
) {
    JSONAssert.assertEquals(
        /* expectedStr = */ expected,
        /* actualStr = */ actual,
        /* compareMode = */ mode,
    )
}

fun compareJson(
    expected: AnyList?,
    actual: AnyList?,
    mode: JSONCompareMode = JSONCompareMode.STRICT,
): JSONCompareResult {
    return compareJson(
        expected = expected?.let(jsonObjectMapper::writeValueAsString),
        actual = actual?.let(jsonObjectMapper::writeValueAsString),
        mode = mode,
    )
}

fun compareJson(
    expected: JsonMap?,
    actual: JsonMap?,
    mode: JSONCompareMode = JSONCompareMode.STRICT,
): JSONCompareResult {
    return compareJson(
        expected = expected?.let(jsonObjectMapper::writeValueAsString),
        actual = actual?.let(jsonObjectMapper::writeValueAsString),
        mode = mode,
    )
}

fun compareJson(
    expected: String?,
    actual: String?,
    mode: JSONCompareMode = JSONCompareMode.STRICT,
): JSONCompareResult {
    return JSONCompare.compareJSON(
        /* expectedStr = */ expected,
        /* actualStr = */ actual,
        /* mode = */ mode,
    )
}
