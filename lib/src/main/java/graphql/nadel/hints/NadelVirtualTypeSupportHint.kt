package graphql.nadel.hints

import graphql.nadel.Service

fun interface NadelVirtualTypeSupportHint {
    /**
     * Determines whether virtual type & some of the new hydration work is enabled.
     */
    operator fun invoke(service: Service): Boolean
}
