package graphql.nadel.hints

import graphql.nadel.Service

fun interface NadelHydrationExecutableSourceFields {
    operator fun invoke(service: Service): Boolean
}
