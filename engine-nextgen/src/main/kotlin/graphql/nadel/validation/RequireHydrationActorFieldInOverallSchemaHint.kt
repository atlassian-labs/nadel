package graphql.nadel.validation

import graphql.nadel.Service

interface RequireHydrationActorFieldInOverallSchemaHint {
    fun getHintValue(service: Service): Boolean
}