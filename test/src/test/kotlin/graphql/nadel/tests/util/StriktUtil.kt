package graphql.nadel.tests.util

import graphql.ExecutionResult
import graphql.GraphQLError
import graphql.nadel.engine.util.AnyMap
import graphql.nadel.engine.util.JsonMap
import graphql.nadel.tests.assertJsonKeys
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

val Assertion.Builder<out ExecutionResult>.extensions: Assertion.Builder<JsonMap>
    get() {
        return get { extensions as AnyMap }.assertJsonKeys()
    }

val Assertion.Builder<out ExecutionResult>.errors: Assertion.Builder<List<GraphQLError>>
    get() {
        return get { errors }
    }

val Assertion.Builder<out GraphQLError>.message: Assertion.Builder<String>
    get() {
        return get { message }
    }

val Assertion.Builder<out ExecutionResult>.data: Assertion.Builder<AnyMap?>
    get() {
        return get {
            getData()
        }
    }

fun Assertion.Builder<out Any>.getHashCode(): Assertion.Builder<Int> {
    return get {
        hashCode()
    }
}

fun Assertion.Builder<out Any>.getToString(): Assertion.Builder<String> {
    return get {
        toString()
    }
}
