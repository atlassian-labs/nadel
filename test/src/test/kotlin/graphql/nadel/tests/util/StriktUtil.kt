package graphql.nadel.tests.util

import strikt.api.Assertion

fun <T : Map<K, V>, K, V> Assertion.Builder<T>.keysEqual(expectedKeys: Collection<K>): Assertion.Builder<T> {
    compose("keys match expected") { actual ->
        val actualKeys = actual.keys
        if (actualKeys != expectedKeys) {
            for (key in actualKeys) {
                if (key !in expectedKeys) {
                    assert("has unexpected $key") {
                        fail()
                    }
                }
            }
            for (key in expectedKeys) {
                if (key !in actualKeys) {
                    assert("missing $key") {
                        fail()
                    }
                }
            }
        }
    } then {
        if (allPassed) pass() else fail()
    }

    return this
}
