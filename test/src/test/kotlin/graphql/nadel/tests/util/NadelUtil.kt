package graphql.nadel.tests.util

import graphql.nadel.Nadel
import graphql.nadel.ServiceExecutionFactory

val Nadel.Builder.serviceExecutionFactory: ServiceExecutionFactory
    get() = getFieldValue("serviceExecutionFactory")

fun <T> Nadel.Builder.getFieldValue(fieldName: String): T {
    @Suppress("UNCHECKED_CAST")
    return javaClass.getDeclaredField(fieldName)
        .also { field ->
            field.isAccessible = true
        }
        .get(this) as T
}
