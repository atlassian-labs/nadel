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
        /* expectedStr = */ expected?.let(jsonObjectMapper::writeValueAsString),
        /* actualStr = */ actual?.let(jsonObjectMapper::writeValueAsString),
        /* compareMode = */ mode,
    )
}

fun assertJsonEquals(
    expected: JsonMap?,
    actual: JsonMap?,
    mode: JSONCompareMode = JSONCompareMode.STRICT,
) {
    assertJsonEquals(
        /* expectedStr = */ expected?.let(jsonObjectMapper::writeValueAsString),
        /* actualStr = */ actual?.let(jsonObjectMapper::writeValueAsString),
        /* compareMode = */ mode,
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
        /* expectedStr = */ expected?.let(jsonObjectMapper::writeValueAsString),
        /* actualStr = */ actual?.let(jsonObjectMapper::writeValueAsString),
        /* compareMode = */ mode,
    )
}

fun compareJson(
    expected: JsonMap?,
    actual: JsonMap?,
    mode: JSONCompareMode = JSONCompareMode.STRICT,
): JSONCompareResult {
    return compareJson(
        /* expectedStr = */ expected?.let(jsonObjectMapper::writeValueAsString),
        /* actualStr = */ actual?.let(jsonObjectMapper::writeValueAsString),
        /* compareMode = */ mode,
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
        /* compareMode = */ mode,
    )
}
