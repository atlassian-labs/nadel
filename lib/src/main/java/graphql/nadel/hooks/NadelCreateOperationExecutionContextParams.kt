package graphql.nadel.hooks

import graphql.nadel.Service
import graphql.nadel.engine.NadelExecutionContext
import graphql.normalized.ExecutableNormalizedField

data class NadelCreateOperationExecutionContextParams internal constructor(
    val executionContext: NadelExecutionContext,
    val service: Service,
    val topLevelField: ExecutableNormalizedField,
)
