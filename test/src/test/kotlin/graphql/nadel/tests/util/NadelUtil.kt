package graphql.nadel.tests.util

import graphql.nadel.Nadel
import graphql.nadel.NadelSchemas
import graphql.nadel.ServiceExecutionFactory

val Nadel.Builder.serviceExecutionFactory: ServiceExecutionFactory
    get() {
        val builder: NadelSchemas.Builder = getValueUsingReflection("schemaBuilder")
        return builder.getValueUsingReflection("serviceExecutionFactory")
    }

fun <T : Any, R> T.getValueUsingReflection(fieldName: String): R {
    @Suppress("UNCHECKED_CAST")
    return javaClass.getDeclaredField(fieldName)
        .also { field ->
            field.isAccessible = true
        }
        .get(this) as R
}
