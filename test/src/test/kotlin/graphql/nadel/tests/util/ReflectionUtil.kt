package graphql.nadel.tests.util

import kotlin.reflect.KProperty1

@Suppress("UNCHECKED_CAST")
fun <R> getPropertyValue(instance: Any, propertyName: String): R {
    val property = instance::class.members
        // don't cast here to <Any, R>, it would succeed silently
        .first { it.name == propertyName } as KProperty1<Any, *>
    // force a invalid cast exception if incorrect type here
    return property.get(instance) as R
}

val Class<*>.packageName: String get() = `package`.name
