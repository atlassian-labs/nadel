package graphql.nadel.tests

import graphql.nadel.engine.util.AnyList
import graphql.nadel.engine.util.AnyMap
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.tests.util.keysEqual
import strikt.api.Assertion
import strikt.assertions.isA

@Deprecated("Do not use")
fun Assertion.Builder<out AnyMap>.assertJsonKeys(): Assertion.Builder<JsonMap> {
    assert("keys are all strings") { subject ->
        @Suppress("UNCHECKED_CAST") // We're checking if the erased type holds up
        for (key in (subject.keys as Set<Any>)) {
            if (key !is String) {
                fail(description = "%s is not a string", actual = key)
                return@assert
            }
        }
        pass()
    }

    @Suppress("UNCHECKED_CAST")
    return this as Assertion.Builder<JsonMap>
}

@Deprecated("Do not use")
private fun Assertion.Builder<JsonMap>.assertJsonObject(expectedMap: JsonMap) {
    keysEqual(expectedMap.keys)

    assertJsonKeys()

    compose("contents match expected") { subjectMap ->
        subjectMap.entries.forEach { (key, subjectValue) ->
            assertJsonEntry(key, subjectValue, expectedValue = expectedMap[key])
        }
    } then {
        if (allPassed || failedCount == 0) pass() else fail()
    }
}

@Deprecated("Do not use")
private fun Assertion.Builder<JsonMap>.assertJsonEntry(key: String, subjectValue: Any?, expectedValue: Any?) {
    get("""entry "$key"""") { subjectValue }
        .assertJsonValue(subjectValue, expectedValue)
}

@Deprecated("Do not use")
private fun jsonTypeOf(element: Any?): String {
    return when (element) {
        is AnyList -> "JSON array"
        is AnyMap -> "JSON object"
        is Boolean -> "boolean"
        is Float -> "float"
        is Double -> "double"
        is Int -> "int"
        is Number -> "number"
        is String -> "string"
        null -> "null"
        else -> element.javaClass.simpleName
    }
}

@Deprecated("Do not use")
private fun <T> Assertion.Builder<T>.assertJsonValue(subjectValue: Any?, expectedValue: Any?) {
    when (subjectValue) {
        is AnyMap -> {
            assert("is same type as expected value") {
                if (expectedValue is AnyMap) {
                    pass()
                    @Suppress("UNCHECKED_CAST")
                    isA<JsonMap>().assertJsonObject(expectedMap = expectedValue as JsonMap)
                } else {
                    fail("expected ${jsonTypeOf(expectedValue)} not ${jsonTypeOf(subjectValue)}")
                }
            }
        }
        is AnyList -> {
            assert("is same type as expected value") {
                if (expectedValue is AnyList) {
                    pass()
                    @Suppress("UNCHECKED_CAST")
                    isA<List<Any>>().assertJsonArray(expectedValue = expectedValue as List<Any>)
                } else {
                    fail("expected ${jsonTypeOf(expectedValue)} not ${jsonTypeOf(subjectValue)}")
                }
            }
        }
        else -> {
            assert("equals expected value") {
                if (subjectValue == expectedValue) {
                    pass()
                } else {
                    fail("""expected "$expectedValue" but got "$subjectValue"""")
                }
            }
        }
    }
}

@Deprecated("Do not use")
private fun <T> Assertion.Builder<List<T>>.assertJsonArray(expectedValue: List<T>) {
    compose("all elements match expected:") { subject ->
        assert("size matches expected") {
            if (subject.size == expectedValue.size) {
                pass()
            } else {
                fail("expected size ${expectedValue.size} but got ${subject.size}")
            }
        }

        subject.forEachIndexed { index, element ->
            if (index > expectedValue.lastIndex) {
                assert("element $index") {
                    fail("is not expected")
                }
            } else {
                get("element $index") { element }
                    .assertJsonValue(subjectValue = element, expectedValue[index])
            }
        }
    } then {
        if (allPassed) pass() else fail()
    }
}
