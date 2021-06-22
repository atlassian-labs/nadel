package graphql.nadel.tests

import graphql.nadel.enginekt.util.AnyList
import graphql.nadel.enginekt.util.AnyMap
import graphql.nadel.enginekt.util.JsonMap
import graphql.nadel.tests.util.keysEqual
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isA

fun assertJsonObject(subject: JsonMap, expected: JsonMap) {
    return expectThat(subject) {
        assertJsonObject(expectedMap = expected)
    }
}

private fun Assertion.Builder<JsonMap>.assertJsonObject(expectedMap: JsonMap) {
    keysEqual(expectedMap.keys)

    assert("keys are all strings") { subject ->
        @Suppress("USELESS_CAST") // We're checking if the erased type holds up
        for (key in (subject.keys as Set<Any>)) {
            if (key !is String) {
                fail(description = "%s is not a string", actual = key)
                return@assert
            }
        }
        pass()
    }

    compose("contents match expected") { subjectMap ->
        subjectMap.entries.forEach { (key, subjectValue) ->
            assertJsonEntry(key, subjectValue, expectedValue = expectedMap[key])
        }
    } then {
        if (allPassed || failedCount == 0) pass() else fail()
    }
}

private fun Assertion.Builder<JsonMap>.assertJsonEntry(key: String, subjectValue: Any?, expectedValue: Any?) {
    get("""entry "$key"""") { subjectValue }
        .assertJsonValue(subjectValue, expectedValue)
}

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

private fun <T> Assertion.Builder<List<T>>.assertJsonArray(expectedValue: List<T>) {
    compose("all elements match expected:") { subject ->
        subject.forEachIndexed { index, element ->
            get("element $index") { element }
                .assertJsonValue(subjectValue = element, expectedValue[index])
        }
    } then {
        if (allPassed) pass() else fail()
    }
    assert("size matches expected") { subject ->
        if (subject.size == expectedValue.size) {
            pass()
        } else {
            fail("expected size ${expectedValue.size} but got ${subject.size}")
        }
    }
}
