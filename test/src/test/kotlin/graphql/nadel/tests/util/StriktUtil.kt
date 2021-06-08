package graphql.nadel.tests.util

import strikt.api.Assertion
import strikt.assertions.containsKey

fun <T : Map<K, V>, K, V> Assertion.Builder<T>.keysEqual(keys: Collection<K>): Assertion.Builder<T> {
    keys.forEach { key ->
        containsKey(key)
    }
    compose("has no extra keys") { subject ->
        subject.keys.forEach { key ->
            assertThat("$key is expected") { key in keys }
        }
    } then {
        if (allPassed || failedCount == 0) pass() else fail()
    }
    return this
}
