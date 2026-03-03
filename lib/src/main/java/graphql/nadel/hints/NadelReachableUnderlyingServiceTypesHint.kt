package graphql.nadel.hints

import graphql.nadel.Service

fun interface NadelReachableUnderlyingServiceTypesHint {
    operator fun invoke(service: Service): Boolean
}
