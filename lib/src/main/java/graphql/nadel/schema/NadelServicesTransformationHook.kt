package graphql.nadel.schema

import graphql.nadel.Service

/**
 * Provides an opportunity to transform the [Service]s right after they're parsed.
 *
 * This hook is invoked before validation etc. so you get the chance to transform the
 * services as if you were editing the raw schema files.
 */
fun interface NadelServicesTransformationHook {
    fun transform(services: List<Service>): List<Service>

    companion object {
        internal val Identity = NadelServicesTransformationHook { it }
    }
}
