package graphql.nadel.engine

import graphql.nadel.Service
import graphql.nadel.ServiceExecutionHydrationDetails
import graphql.nadel.hooks.NadelCreateOperationExecutionContextParams
import graphql.normalized.ExecutableNormalizedField

/**
 * Base class for context objects created for one service execution.
 *
 * This is created for each portion of the query we execute.
 *
 * This is NOT shared with other executions to the same service.
 */
abstract class NadelOperationExecutionContext {
    abstract val parentContext: NadelExecutionContext
    abstract val service: Service
    abstract val topLevelField: ExecutableNormalizedField

    val executionContext: NadelExecutionContext get() = parentContext
    val userContext: Any? get() = executionContext.userContext
    val hydrationDetails: ServiceExecutionHydrationDetails? get() = executionContext.hydrationDetails

    internal class Default(
        override val parentContext: NadelExecutionContext,
        override val service: Service,
        override val topLevelField: ExecutableNormalizedField,
    ) : NadelOperationExecutionContext()

    companion object {
        internal fun from(params: NadelCreateOperationExecutionContextParams): NadelOperationExecutionContext {
            return Default(
                parentContext = params.executionContext,
                service = params.service,
                topLevelField = params.topLevelField,
            )
        }
    }
}
