package graphql.nadel.engine

import graphql.nadel.NadelOperationExecutionHydrationDetails
import graphql.nadel.Service
import graphql.nadel.hooks.NadelCreateOperationExecutionContextParams
import graphql.normalized.ExecutableNormalizedField

/**
 * Base class for context objects created for one service operation execution.
 *
 * There may be multiple instances of this per call to [Nadel]
 */
abstract class NadelOperationExecutionContext {
    abstract val parentContext: NadelExecutionContext
    abstract val service: Service
    abstract val topLevelField: ExecutableNormalizedField

    abstract val hydrationDetails: NadelOperationExecutionHydrationDetails?
    abstract val isPartitionedCall: Boolean

    val executionContext: NadelExecutionContext get() = parentContext
    val userContext: Any? get() = executionContext.userContext

    internal class Default(
        override val parentContext: NadelExecutionContext,
        override val service: Service,
        override val topLevelField: ExecutableNormalizedField,
        override val hydrationDetails: NadelOperationExecutionHydrationDetails?,
        override val isPartitionedCall: Boolean,
    ) : NadelOperationExecutionContext()

    companion object {
        internal fun from(params: NadelCreateOperationExecutionContextParams): NadelOperationExecutionContext {
            return Default(
                parentContext = params.executionContext,
                service = params.service,
                topLevelField = params.topLevelField,
                hydrationDetails = params.hydrationDetails,
                isPartitionedCall = params.isPartitionedCall,
            )
        }
    }
}
