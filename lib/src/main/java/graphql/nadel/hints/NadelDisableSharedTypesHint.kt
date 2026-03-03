package graphql.nadel.hints

import graphql.nadel.Service

fun interface NadelDisableSharedTypesHint {
    operator fun invoke(service: Service): Boolean
}
