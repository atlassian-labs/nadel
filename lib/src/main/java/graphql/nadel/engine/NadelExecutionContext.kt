package graphql.nadel.engine

import graphql.ExecutionInput
import graphql.GraphQLContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.nadel.NadelExecutionHints
import graphql.nadel.engine.blueprint.NadelOverallExecutionBlueprint
import graphql.nadel.engine.instrumentation.NadelInstrumentationTimer
import graphql.nadel.hooks.NadelExecutionHooks
import graphql.nadel.result.NadelResultTracker
import graphql.normalized.ExecutableNormalizedOperation
import graphql.schema.GraphQLSchema
import kotlinx.coroutines.CoroutineScope

data class NadelExecutionContext internal constructor(
    val executionBlueprint: NadelOverallExecutionBlueprint,
    val executionInput: ExecutionInput,
    val query: ExecutableNormalizedOperation,
    internal val hooks: NadelExecutionHooks,
    val hints: NadelExecutionHints,
    val instrumentationState: InstrumentationState?,
    internal val timer: NadelInstrumentationTimer,
    internal val incrementalResultSupport: NadelIncrementalResultSupport,
    internal val resultTracker: NadelResultTracker,
    internal val executionCoroutine: CoroutineScope,
) {
    val userContext: Any?
        get() = executionInput.context

    val graphQLContext: GraphQLContext
        get() = executionInput.graphQLContext!!

    val engineSchema: GraphQLSchema
        get() = executionBlueprint.engineSchema
}
