package graphql.nadel.engine.transform

import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelOperationExecutionContext

abstract class NadelTransformOperationContext : NadelTransformContext {
    abstract val parentContext: NadelOperationExecutionContext

    override val operationExecutionContext: NadelOperationExecutionContext = parentContext
    override val executionContext: NadelExecutionContext = parentContext.parentContext
}
