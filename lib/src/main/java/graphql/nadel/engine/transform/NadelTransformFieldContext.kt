package graphql.nadel.engine.transform

import graphql.nadel.engine.NadelExecutionContext
import graphql.nadel.engine.NadelOperationExecutionContext
import graphql.normalized.ExecutableNormalizedField

abstract class NadelTransformFieldContext<TransformOperationContext : NadelTransformOperationContext> :
    NadelTransformContext {
    abstract val parentContext: TransformOperationContext
    abstract val overallField: ExecutableNormalizedField

    override val operationExecutionContext: NadelOperationExecutionContext = parentContext.parentContext
    override val executionContext: NadelExecutionContext = parentContext.parentContext.parentContext

    val transformOperationContext: TransformOperationContext get() = parentContext
}
